package com.studyflow.media;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.media.dto.MediaAttachmentResponse;
import com.studyflow.media.dto.MediaUploadCompleteResponse;
import com.studyflow.media.dto.MediaUploadPrepareRequest;
import com.studyflow.media.dto.MediaUploadPrepareResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MediaService {
    private static final String STORAGE_PROVIDER_R2 = "R2";
    private static final String FILE_TYPE_IMAGE = "IMAGE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_ATTACHED = "ATTACHED";
    private static final int MAX_POST_MEDIA_COUNT = 9;

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final MediaFileMapper mediaFileMapper;
    private final CommunityPostMediaMapper communityPostMediaMapper;
    private final CommunityMemberService communityMemberService;
    private final R2StorageProperties r2StorageProperties;

    public MediaService(
            MediaFileMapper mediaFileMapper,
            CommunityPostMediaMapper communityPostMediaMapper,
            CommunityMemberService communityMemberService,
            R2StorageProperties r2StorageProperties
    ) {
        this.mediaFileMapper = mediaFileMapper;
        this.communityPostMediaMapper = communityPostMediaMapper;
        this.communityMemberService = communityMemberService;
        this.r2StorageProperties = r2StorageProperties;
    }

    @Transactional
    public MediaUploadPrepareResponse prepareImageUpload(Long userId, MediaUploadPrepareRequest request) {
        communityMemberService.requireActiveDefaultMember(userId);
        requireR2Configured();

        String contentType = normalizeContentType(request.contentType());
        validateImageUpload(contentType, request.fileSize());

        LocalDateTime now = LocalDateTime.now();
        String objectKey = buildObjectKey(userId, request.filename(), contentType, now);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setUploaderId(userId);
        mediaFile.setStorageProvider(STORAGE_PROVIDER_R2);
        mediaFile.setBucketName(r2StorageProperties.getBucket());
        mediaFile.setObjectKey(objectKey);
        mediaFile.setOriginalFilename(safeFilename(request.filename()));
        mediaFile.setContentType(contentType);
        mediaFile.setFileType(FILE_TYPE_IMAGE);
        mediaFile.setFileSize(request.fileSize());
        mediaFile.setStatus(STATUS_PENDING);
        mediaFile.setCreatedAt(now);
        mediaFile.setUpdatedAt(now);
        mediaFileMapper.insert(mediaFile);

        LocalDateTime expiresAt = now.plus(r2StorageProperties.getUploadUrlTtl());
        String uploadUrl = presignPutUrl(objectKey, contentType);
        return new MediaUploadPrepareResponse(
                mediaFile.getId(),
                objectKey,
                uploadUrl,
                Map.of("Content-Type", contentType),
                contentType,
                r2StorageProperties.getMaxImageBytes(),
                expiresAt
        );
    }

    @Transactional
    public MediaUploadCompleteResponse completeUpload(Long userId, Long mediaFileId) {
        communityMemberService.requireActiveDefaultMember(userId);
        MediaFile mediaFile = requireOwnMedia(mediaFileId, userId);
        if (STATUS_PENDING.equals(mediaFile.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            int updated = mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                    .eq(MediaFile::getId, mediaFile.getId())
                    .eq(MediaFile::getUploaderId, userId)
                    .eq(MediaFile::getStatus, STATUS_PENDING)
                    .set(MediaFile::getStatus, STATUS_UPLOADED)
                    .set(MediaFile::getUploadedAt, now)
                    .set(MediaFile::getUpdatedAt, now));
            if (updated != 1) {
                throw new BusinessException(409, "文件状态已变化");
            }
            mediaFile.setStatus(STATUS_UPLOADED);
            mediaFile.setUploadedAt(now);
            mediaFile.setUpdatedAt(now);
        }
        return toCompleteResponse(mediaFile);
    }

    @Transactional
    public void replacePostMedia(Long userId, Long postId, List<Long> mediaFileIds, LocalDateTime now) {
        if (mediaFileIds == null) {
            return;
        }
        List<Long> normalizedIds = normalizeMediaIds(mediaFileIds);
        communityPostMediaMapper.delete(new LambdaQueryWrapper<CommunityPostMedia>()
                .eq(CommunityPostMedia::getPostId, postId));
        if (normalizedIds.isEmpty()) {
            return;
        }

        Map<Long, MediaFile> mediaById = ownAttachableMedia(userId, normalizedIds);
        for (Long mediaFileId : normalizedIds) {
            MediaFile mediaFile = mediaById.get(mediaFileId);
            if (mediaFile == null) {
                throw new BusinessException(400, "图片不存在或还没有上传完成");
            }
        }

        for (int index = 0; index < normalizedIds.size(); index++) {
            CommunityPostMedia postMedia = new CommunityPostMedia();
            postMedia.setPostId(postId);
            postMedia.setMediaFileId(normalizedIds.get(index));
            postMedia.setSortOrder(index);
            postMedia.setCreatedAt(now);
            communityPostMediaMapper.insert(postMedia);
        }

        mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .in(MediaFile::getId, normalizedIds)
                .eq(MediaFile::getUploaderId, userId)
                .set(MediaFile::getStatus, STATUS_ATTACHED)
                .set(MediaFile::getUpdatedAt, now));
    }

    public Map<Long, List<MediaAttachmentResponse>> attachmentsByPostIds(Set<Long> postIds) {
        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CommunityPostMedia> postMediaRows = communityPostMediaMapper.selectList(
                new LambdaQueryWrapper<CommunityPostMedia>()
                        .in(CommunityPostMedia::getPostId, postIds)
                        .orderByAsc(CommunityPostMedia::getPostId)
                        .orderByAsc(CommunityPostMedia::getSortOrder)
                        .orderByAsc(CommunityPostMedia::getId)
        );
        if (postMediaRows.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> mediaFileIds = postMediaRows.stream()
                .map(CommunityPostMedia::getMediaFileId)
                .distinct()
                .toList();
        Map<Long, MediaFile> mediaById = mediaFileMapper.selectBatchIds(mediaFileIds)
                .stream()
                .collect(Collectors.toMap(MediaFile::getId, Function.identity()));

        Map<Long, List<MediaAttachmentResponse>> result = new LinkedHashMap<>();
        for (CommunityPostMedia postMedia : postMediaRows) {
            MediaFile mediaFile = mediaById.get(postMedia.getMediaFileId());
            if (mediaFile == null) {
                continue;
            }
            result.computeIfAbsent(postMedia.getPostId(), ignored -> new ArrayList<>())
                    .add(toAttachmentResponse(mediaFile));
        }
        return result;
    }

    private Map<Long, MediaFile> ownAttachableMedia(Long userId, List<Long> mediaFileIds) {
        return mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                        .eq(MediaFile::getUploaderId, userId)
                        .in(MediaFile::getId, mediaFileIds)
                        .in(MediaFile::getStatus, List.of(STATUS_UPLOADED, STATUS_ATTACHED)))
                .stream()
                .collect(Collectors.toMap(MediaFile::getId, Function.identity()));
    }

    private List<Long> normalizeMediaIds(List<Long> mediaFileIds) {
        LinkedHashSet<Long> distinctIds = mediaFileIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.size() > MAX_POST_MEDIA_COUNT) {
            throw new BusinessException(400, "一条动态最多上传 9 张图片");
        }
        return List.copyOf(distinctIds);
    }

    private MediaFile requireOwnMedia(Long mediaFileId, Long userId) {
        MediaFile mediaFile = mediaFileMapper.selectOne(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaFileId)
                .eq(MediaFile::getUploaderId, userId));
        if (mediaFile == null) {
            throw new BusinessException(404, "文件不存在");
        }
        return mediaFile;
    }

    private void requireR2Configured() {
        if (!r2StorageProperties.isConfigured()) {
            throw new BusinessException(503, "媒体存储还没有配置");
        }
    }

    private void validateImageUpload(String contentType, Long fileSize) {
        if (!SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(400, "目前只支持 JPG、PNG、WebP、GIF 图片");
        }
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(400, "文件大小不正确");
        }
        if (fileSize > r2StorageProperties.getMaxImageBytes()) {
            throw new BusinessException(400, "图片不能超过 10MB");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String buildObjectKey(Long userId, String filename, String contentType, LocalDateTime now) {
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return "community/images/%d/%s/%s.%s".formatted(
                userId,
                datePath,
                UUID.randomUUID(),
                extension(filename, contentType)
        );
    }

    private String extension(String filename, String contentType) {
        String safeName = safeFilename(filename).toLowerCase(Locale.ROOT);
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < safeName.length() - 1) {
            String ext = safeName.substring(dotIndex + 1);
            if (Set.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
                return ext;
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> "bin";
        };
    }

    private String safeFilename(String filename) {
        String value = filename == null ? "upload" : filename.trim();
        if (value.isBlank()) {
            return "upload";
        }
        return value.replaceAll("[\\\\/\\r\\n\\t]", "_");
    }

    private String presignPutUrl(String objectKey, String contentType) {
        try (S3Presigner presigner = createPresigner()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2StorageProperties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            PresignedPutObjectRequest request = presigner.presignPutObject(builder -> builder
                    .signatureDuration(r2StorageProperties.getUploadUrlTtl())
                    .putObjectRequest(putObjectRequest));
            return request.url().toString();
        }
    }

    private String presignGetUrl(MediaFile mediaFile) {
        try (S3Presigner presigner = createPresigner()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(mediaFile.getBucketName())
                    .key(mediaFile.getObjectKey())
                    .responseContentType(mediaFile.getContentType())
                    .build();
            PresignedGetObjectRequest request = presigner.presignGetObject(builder -> builder
                    .signatureDuration(r2StorageProperties.getReadUrlTtl())
                    .getObjectRequest(getObjectRequest));
            return request.url().toString();
        }
    }

    private S3Presigner createPresigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                r2StorageProperties.getAccessKeyId(),
                r2StorageProperties.getSecretAccessKey()
        );
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .endpointOverride(URI.create(r2StorageProperties.endpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private MediaAttachmentResponse toAttachmentResponse(MediaFile mediaFile) {
        return new MediaAttachmentResponse(
                mediaFile.getId(),
                mediaFile.getFileType(),
                mediaFile.getContentType(),
                mediaFile.getOriginalFilename(),
                mediaFile.getFileSize(),
                presignGetUrl(mediaFile)
        );
    }

    private MediaUploadCompleteResponse toCompleteResponse(MediaFile mediaFile) {
        return new MediaUploadCompleteResponse(
                mediaFile.getId(),
                mediaFile.getFileType(),
                mediaFile.getContentType(),
                mediaFile.getOriginalFilename(),
                mediaFile.getFileSize(),
                mediaFile.getStatus()
        );
    }
}
