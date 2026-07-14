package com.studyflow.media;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MediaTranscodeService {
    private static final Logger log = LoggerFactory.getLogger(MediaTranscodeService.class);
    private static final String FILE_TYPE_VIDEO = "VIDEO";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String TRANSCODE_TRANSCODING = "TRANSCODING";
    private static final String TRANSCODE_READY = "READY";
    private static final String TRANSCODE_FAILED = "FAILED";
    private static final String VARIANT_STATUS_READY = "READY";
    private static final String CONTENT_TYPE_M3U8 = "application/vnd.apple.mpegurl";
    private static final String CONTENT_TYPE_TS = "video/mp2t";
    private static final int MAX_ERROR_LENGTH = 1000;

    private static final List<VariantSpec> VARIANTS = List.of(
            new VariantSpec("480P", 640, 480, 1000),
            new VariantSpec("720P", 1280, 720, 2800),
            new VariantSpec("1080P", 1920, 1080, 5000)
    );

    private final MediaFileMapper mediaFileMapper;
    private final MediaTranscodeVariantMapper mediaTranscodeVariantMapper;
    private final MediaTranscodeSegmentMapper mediaTranscodeSegmentMapper;
    private final R2StorageProperties r2StorageProperties;
    private final MediaTranscodeProperties mediaTranscodeProperties;
    private final MediaTranscodePlaylistParser playlistParser;
    private final MediaTranscodeTaskPublisher taskPublisher;

    public MediaTranscodeService(
            MediaFileMapper mediaFileMapper,
            MediaTranscodeVariantMapper mediaTranscodeVariantMapper,
            MediaTranscodeSegmentMapper mediaTranscodeSegmentMapper,
            R2StorageProperties r2StorageProperties,
            MediaTranscodeProperties mediaTranscodeProperties,
            MediaTranscodePlaylistParser playlistParser,
            MediaTranscodeTaskPublisher taskPublisher
    ) {
        this.mediaFileMapper = mediaFileMapper;
        this.mediaTranscodeVariantMapper = mediaTranscodeVariantMapper;
        this.mediaTranscodeSegmentMapper = mediaTranscodeSegmentMapper;
        this.r2StorageProperties = r2StorageProperties;
        this.mediaTranscodeProperties = mediaTranscodeProperties;
        this.playlistParser = playlistParser;
        this.taskPublisher = taskPublisher;
    }

    public void requestTranscode(Long mediaFileId) {
        if (!mediaTranscodeProperties.isEnabled()) {
            log.info("Media transcode is disabled, mediaFileId={} stays queued", mediaFileId);
            return;
        }
        taskPublisher.publishTranscodeRequested(mediaFileId);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTranscodeRequested(MediaTranscodeRequestedEvent event) {
        transcodeVideo(event.mediaFileId());
    }

    void transcodeVideo(Long mediaFileId) {
        Path workDir = mediaTranscodeProperties.getWorkDir()
                .resolve("media-" + mediaFileId + "-" + UUID.randomUUID());
        try {
            MediaFile mediaFile = mediaFileMapper.selectById(mediaFileId);
            if (mediaFile == null
                    || !FILE_TYPE_VIDEO.equals(mediaFile.getFileType())
                    || !STATUS_APPROVED.equals(mediaFile.getStatus())) {
                log.warn("Skip transcode for unavailable media, mediaFileId={}", mediaFileId);
                return;
            }

            markTranscoding(mediaFileId);
            Files.createDirectories(workDir);
            Path sourcePath = workDir.resolve("source." + extension(mediaFile.getOriginalFilename(), mediaFile.getContentType()));

            try (S3Client s3Client = createS3Client()) {
                downloadSource(s3Client, mediaFile, sourcePath);
                clearOldTranscodeResult(mediaFileId);

                Integer durationSeconds = null;
                List<MediaTranscodeVariant> variantRecords = new ArrayList<>();
                for (VariantSpec variant : VARIANTS) {
                    Path variantDir = workDir.resolve(normalizeQualityPath(variant.qualityLabel()));
                    Files.createDirectories(variantDir);
                    Path playlistPath = variantDir.resolve("index.m3u8");
                    runFfmpeg(sourcePath, variantDir, playlistPath, variant);
                    MediaTranscodeVariant record = uploadVariant(s3Client, mediaFile, variant, variantDir, playlistPath);
                    variantRecords.add(record);
                    if (durationSeconds == null) {
                        durationSeconds = estimateDurationSeconds(playlistPath);
                    }
                }

                uploadMasterPlaylist(s3Client, mediaFile, variantRecords);
                markReady(mediaFileId, durationSeconds);
            }
        } catch (Exception exception) {
            log.warn("Video transcode failed, mediaFileId={}", mediaFileId, exception);
            markFailed(mediaFileId, exception.getMessage());
        } finally {
            deleteQuietly(workDir);
        }
    }

    private void markTranscoding(Long mediaFileId) {
        LocalDateTime now = LocalDateTime.now();
        mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaFileId)
                .set(MediaFile::getTranscodeStatus, TRANSCODE_TRANSCODING)
                .set(MediaFile::getTranscodeError, null)
                .set(MediaFile::getTranscodeStartedAt, now)
                .set(MediaFile::getTranscodeCompletedAt, null)
                .set(MediaFile::getUpdatedAt, now));
    }

    private void clearOldTranscodeResult(Long mediaFileId) {
        mediaTranscodeSegmentMapper.delete(new LambdaQueryWrapper<MediaTranscodeSegment>()
                .eq(MediaTranscodeSegment::getMediaFileId, mediaFileId));
        mediaTranscodeVariantMapper.delete(new LambdaQueryWrapper<MediaTranscodeVariant>()
                .eq(MediaTranscodeVariant::getMediaFileId, mediaFileId));
    }

    private void markReady(Long mediaFileId, Integer durationSeconds) {
        LocalDateTime now = LocalDateTime.now();
        mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaFileId)
                .set(MediaFile::getTranscodeStatus, TRANSCODE_READY)
                .set(MediaFile::getTranscodeError, null)
                .set(MediaFile::getTranscodeCompletedAt, now)
                .set(MediaFile::getHlsMasterObjectKey, "community/videos/%d/hls/master.m3u8".formatted(mediaFileId))
                .set(MediaFile::getDurationSeconds, durationSeconds)
                .set(MediaFile::getUpdatedAt, now));
    }

    private void markFailed(Long mediaFileId, String message) {
        LocalDateTime now = LocalDateTime.now();
        mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaFileId)
                .set(MediaFile::getTranscodeStatus, TRANSCODE_FAILED)
                .set(MediaFile::getTranscodeError, trimError(message))
                .set(MediaFile::getTranscodeCompletedAt, now)
                .set(MediaFile::getUpdatedAt, now));
    }

    private void downloadSource(S3Client s3Client, MediaFile mediaFile, Path sourcePath) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(mediaFile.getBucketName())
                .key(mediaFile.getObjectKey())
                .build();
        s3Client.getObject(request, ResponseTransformer.toFile(sourcePath));
    }

    private void runFfmpeg(Path sourcePath, Path variantDir, Path playlistPath, VariantSpec variant)
            throws IOException, InterruptedException {
        Path segmentPattern = variantDir.resolve("segment-%03d.ts");
        List<String> command = List.of(
                mediaTranscodeProperties.getFfmpegPath(),
                "-y",
                "-i", sourcePath.toString(),
                "-vf", "scale=w=%d:h=-2:force_original_aspect_ratio=decrease".formatted(variant.width()),
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "128k",
                "-f", "hls",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segmentPattern.toString(),
                playlistPath.toString()
        );

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(mediaTranscodeProperties.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("FFmpeg timeout for " + variant.qualityLabel());
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("FFmpeg failed for %s: %s".formatted(variant.qualityLabel(), tail(output)));
        }
    }

    private MediaTranscodeVariant uploadVariant(
            S3Client s3Client,
            MediaFile mediaFile,
            VariantSpec variant,
            Path variantDir,
            Path playlistPath
    ) throws IOException {
        String qualityPath = normalizeQualityPath(variant.qualityLabel());
        String playlistObjectKey = "community/videos/%d/hls/%s/index.m3u8".formatted(mediaFile.getId(), qualityPath);
        uploadObject(s3Client, mediaFile.getBucketName(), playlistObjectKey, playlistPath, CONTENT_TYPE_M3U8);

        MediaTranscodeVariant variantRecord = new MediaTranscodeVariant();
        variantRecord.setMediaFileId(mediaFile.getId());
        variantRecord.setQualityLabel(variant.qualityLabel());
        variantRecord.setWidth(variant.width());
        variantRecord.setHeight(variant.height());
        variantRecord.setBitrateKbps(variant.bitrateKbps());
        variantRecord.setPlaylistObjectKey(playlistObjectKey);
        variantRecord.setStatus(VARIANT_STATUS_READY);
        mediaTranscodeVariantMapper.insert(variantRecord);

        for (MediaTranscodePlaylistParser.ParsedSegment segment : playlistParser.parse(playlistPath)) {
            Path segmentPath = variantDir.resolve(segment.filename());
            String segmentObjectKey = "community/videos/%d/hls/%s/%s".formatted(
                    mediaFile.getId(),
                    qualityPath,
                    segment.filename()
            );
            uploadObject(s3Client, mediaFile.getBucketName(), segmentObjectKey, segmentPath, CONTENT_TYPE_TS);

            MediaTranscodeSegment segmentRecord = new MediaTranscodeSegment();
            segmentRecord.setMediaFileId(mediaFile.getId());
            segmentRecord.setQualityLabel(variant.qualityLabel());
            segmentRecord.setSegmentIndex(segment.index());
            segmentRecord.setDurationSeconds(segment.durationSeconds());
            segmentRecord.setObjectKey(segmentObjectKey);
            segmentRecord.setByteSize(Files.size(segmentPath));
            mediaTranscodeSegmentMapper.insert(segmentRecord);
        }
        return variantRecord;
    }

    private void uploadMasterPlaylist(S3Client s3Client, MediaFile mediaFile, List<MediaTranscodeVariant> variants) {
        StringBuilder content = new StringBuilder();
        content.append("#EXTM3U\n");
        content.append("#EXT-X-VERSION:3\n");
        for (MediaTranscodeVariant variant : variants) {
            content.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(variant.getBitrateKbps() != null ? variant.getBitrateKbps() * 1000 : 0)
                    .append(",RESOLUTION=")
                    .append(variant.getWidth())
                    .append("x")
                    .append(variant.getHeight())
                    .append("\n");
            String qualityPath = normalizeQualityPath(variant.getQualityLabel());
            content.append(qualityPath).append("/index.m3u8\n");
        }

        String masterKey = "community/videos/%d/hls/master.m3u8".formatted(mediaFile.getId());
        uploadTextObject(s3Client, mediaFile.getBucketName(), masterKey, content.toString(), CONTENT_TYPE_M3U8);
    }

    private void uploadTextObject(S3Client s3Client, String bucketName, String objectKey, String content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));
    }

    private void uploadObject(S3Client s3Client, String bucketName, String objectKey, Path path, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromFile(path));
    }

    private Integer estimateDurationSeconds(Path playlistPath) {
        BigDecimal total = playlistParser.parse(playlistPath).stream()
                .map(MediaTranscodePlaylistParser.ParsedSegment::durationSeconds)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(0, java.math.RoundingMode.CEILING).intValue();
    }

    private S3Client createS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                r2StorageProperties.getAccessKeyId(),
                r2StorageProperties.getSecretAccessKey()
        );
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .endpointOverride(URI.create(r2StorageProperties.endpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private String extension(String filename, String contentType) {
        String safeName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < safeName.length() - 1) {
            String ext = safeName.substring(dotIndex + 1);
            if (List.of("mp4", "webm", "mov", "mkv").contains(ext)) {
                return ext;
            }
        }
        if ("video/mp4".equals(contentType)) {
            return "mp4";
        }
        if ("video/webm".equals(contentType)) {
            return "webm";
        }
        return "mp4";
    }

    private String normalizeQualityPath(String qualityLabel) {
        return qualityLabel.trim().toLowerCase(Locale.ROOT);
    }

    private String trimError(String message) {
        String value = message == null || message.isBlank() ? "unknown transcode error" : message;
        return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
    }

    private String tail(String value) {
        if (value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(value.length() - MAX_ERROR_LENGTH);
    }

    private void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException exception) {
                    log.debug("Failed to delete temp transcode file {}", item, exception);
                }
            });
        } catch (IOException exception) {
            log.debug("Failed to walk temp transcode dir {}", path, exception);
        }
    }

    private record VariantSpec(
            String qualityLabel,
            int width,
            int height,
            int bitrateKbps
    ) {
    }
}
