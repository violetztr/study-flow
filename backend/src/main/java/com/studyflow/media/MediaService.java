package com.studyflow.media;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.media.dto.MediaAttachmentResponse;
import com.studyflow.media.dto.MediaUploadCompleteResponse;
import com.studyflow.media.dto.MediaUploadPrepareRequest;
import com.studyflow.media.dto.MediaUploadPrepareResponse;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
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
    private static final String FILE_TYPE_VIDEO = "VIDEO";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_UPLOADED = "UPLOADED";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_ATTACHED = "ATTACHED";
    private static final String RURU_ADMIN_USERNAME = "ruru";
    private static final int MAX_POST_MEDIA_COUNT = 9;

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm"
    );

    private final MediaFileMapper mediaFileMapper;
    private final CommunityPostMediaMapper communityPostMediaMapper;
    private final CommunityMemberService communityMemberService;
    private final R2StorageProperties r2StorageProperties;
    private final UserMapper userMapper;

    public MediaService(
            MediaFileMapper mediaFileMapper,
            CommunityPostMediaMapper communityPostMediaMapper,
            CommunityMemberService communityMemberService,
            R2StorageProperties r2StorageProperties,
            UserMapper userMapper
    ) {
        this.mediaFileMapper = mediaFileMapper;
        this.communityPostMediaMapper = communityPostMediaMapper;
        this.communityMemberService = communityMemberService;
        this.r2StorageProperties = r2StorageProperties;
        this.userMapper = userMapper;
    }

    @Transactional
    public MediaUploadPrepareResponse prepareUpload(Long userId, MediaUploadPrepareRequest request) {
        communityMemberService.requireActiveDefaultMember(userId);
        requireR2Configured();

        String contentType = normalizeContentType(request.contentType());
        String fileType = resolveFileType(contentType);
        validateUpload(fileType, contentType, request.fileSize());

        LocalDateTime now = LocalDateTime.now();
        String objectKey = buildObjectKey(userId, request.filename(), fileType, contentType, now);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setUploaderId(userId);
        mediaFile.setStorageProvider(STORAGE_PROVIDER_R2);
        mediaFile.setBucketName(r2StorageProperties.getBucket());
        mediaFile.setObjectKey(objectKey);
        mediaFile.setOriginalFilename(safeFilename(request.filename()));
        mediaFile.setContentType(contentType);
        mediaFile.setFileType(fileType);
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
                maxBytes(fileType),
                expiresAt
        );
    }

    @Transactional
    public MediaUploadCompleteResponse completeUpload(Long userId, Long mediaFileId) {
        communityMemberService.requireActiveDefaultMember(userId);
        MediaFile mediaFile = requireOwnMedia(mediaFileId, userId);
        if (STATUS_PENDING.equals(mediaFile.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            String completedStatus = FILE_TYPE_VIDEO.equals(mediaFile.getFileType())
                    ? STATUS_PENDING_REVIEW
                    : STATUS_UPLOADED;
            int updated = mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                    .eq(MediaFile::getId, mediaFile.getId())
                    .eq(MediaFile::getUploaderId, userId)
                    .eq(MediaFile::getStatus, STATUS_PENDING)
                    .set(MediaFile::getStatus, completedStatus)
                    .set(MediaFile::getUploadedAt, now)
                    .set(MediaFile::getUpdatedAt, now));
            if (updated != 1) {
                throw new BusinessException(409, "文件状态已变化");
            }
            mediaFile.setStatus(completedStatus);
            mediaFile.setUploadedAt(now);
            mediaFile.setUpdatedAt(now);
        }
        return toCompleteResponse(mediaFile);
    }

    public List<MediaUploadCompleteResponse> listPendingReviewMedia(Long adminUserId) {
        requireRuruAdmin(adminUserId);
        return mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                        .eq(MediaFile::getFileType, FILE_TYPE_VIDEO)
                        .eq(MediaFile::getStatus, STATUS_PENDING_REVIEW)
                        .orderByAsc(MediaFile::getCreatedAt)
                        .orderByAsc(MediaFile::getId))
                .stream()
                .map(this::toCompleteResponse)
                .toList();
    }

    @Transactional
    public MediaUploadCompleteResponse approveVideo(Long adminUserId, Long mediaFileId) {
        requireRuruAdmin(adminUserId);
        MediaFile mediaFile = requireVideoReviewMedia(mediaFileId);
        LocalDateTime now = LocalDateTime.now();
        updateReviewStatus(mediaFile.getId(), STATUS_APPROVED, now);
        mediaFile.setStatus(STATUS_APPROVED);
        mediaFile.setUpdatedAt(now);
        return toCompleteResponse(mediaFile);
    }

    @Transactional
    public MediaUploadCompleteResponse rejectVideo(Long adminUserId, Long mediaFileId) {
        requireRuruAdmin(adminUserId);
        MediaFile mediaFile = requireVideoReviewMedia(mediaFileId);
        LocalDateTime now = LocalDateTime.now();
        updateReviewStatus(mediaFile.getId(), STATUS_REJECTED, now);
        mediaFile.setStatus(STATUS_REJECTED);
        mediaFile.setUpdatedAt(now);
        return toCompleteResponse(mediaFile);
    }

    @Transactional
    public void replacePostMedia(
            Long userId,
            Long postId,
            List<Long> mediaFileIds,
            Long videoCoverMediaFileId,
            LocalDateTime now
    ) {
        if (mediaFileIds == null) {
            if (videoCoverMediaFileId != null) {
                throw new BusinessException(400, "Only video posts need a cover");
            }
            return;
        }
        List<Long> normalizedIds = normalizeMediaIds(mediaFileIds);
        communityPostMediaMapper.delete(new LambdaQueryWrapper<CommunityPostMedia>()
                .eq(CommunityPostMedia::getPostId, postId));
        if (normalizedIds.isEmpty()) {
            if (videoCoverMediaFileId != null) {
                throw new BusinessException(400, "Only video posts need a cover");
            }
            return;
        }

        Map<Long, MediaFile> mediaById = ownAttachableMedia(userId, normalizedIds);
        for (Long mediaFileId : normalizedIds) {
            MediaFile mediaFile = mediaById.get(mediaFileId);
            if (mediaFile == null) {
                throw new BusinessException(400, "媒体不存在或还没有上传完成");
            }
        }
        long videoCount = mediaById.values().stream()
                .filter(mediaFile -> FILE_TYPE_VIDEO.equals(mediaFile.getFileType()))
                .count();
        MediaFile videoCoverMedia = resolveVideoCover(userId, videoCoverMediaFileId, videoCount);
        if (videoCount > 1) {
            throw new BusinessException(400, "一条动态最多上传 1 个视频");
        }

        for (int index = 0; index < normalizedIds.size(); index++) {
            CommunityPostMedia postMedia = new CommunityPostMedia();
            postMedia.setPostId(postId);
            postMedia.setMediaFileId(normalizedIds.get(index));
            postMedia.setSortOrder(index);
            postMedia.setCreatedAt(now);
            communityPostMediaMapper.insert(postMedia);
        }

        LinkedHashSet<Long> imageIds = mediaById.values().stream()
                .filter(mediaFile -> FILE_TYPE_IMAGE.equals(mediaFile.getFileType()))
                .map(MediaFile::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (videoCoverMedia != null) {
            imageIds.add(videoCoverMedia.getId());
        }
        if (!imageIds.isEmpty()) {
            mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                    .in(MediaFile::getId, imageIds)
                    .eq(MediaFile::getUploaderId, userId)
                    .set(MediaFile::getStatus, STATUS_ATTACHED)
                    .set(MediaFile::getUpdatedAt, now));
        }
    }

    public Map<Long, List<MediaAttachmentResponse>> attachmentsByPostIds(Set<Long> postIds) {
        return attachmentsByPostIds(postIds, Collections.emptyMap());
    }

    public Map<Long, List<MediaAttachmentResponse>> attachmentsByPostIds(
            Set<Long> postIds,
            Map<Long, Long> videoCoverMediaFileIds
    ) {
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

        LinkedHashSet<Long> mediaFileIds = postMediaRows.stream()
                .map(CommunityPostMedia::getMediaFileId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        videoCoverMediaFileIds.values().stream()
                .filter(id -> id != null && id > 0)
                .forEach(mediaFileIds::add);
        Map<Long, MediaFile> mediaById = mediaFileMapper.selectBatchIds(mediaFileIds)
                .stream()
                .collect(Collectors.toMap(MediaFile::getId, Function.identity()));

        Map<Long, List<MediaAttachmentResponse>> result = new LinkedHashMap<>();
        for (CommunityPostMedia postMedia : postMediaRows) {
            MediaFile mediaFile = mediaById.get(postMedia.getMediaFileId());
            if (mediaFile == null || !isPubliclyVisible(mediaFile)) {
                continue;
            }
            MediaFile coverMediaFile = null;
            if (FILE_TYPE_VIDEO.equals(mediaFile.getFileType())) {
                coverMediaFile = mediaById.get(videoCoverMediaFileIds.get(postMedia.getPostId()));
            }
            result.computeIfAbsent(postMedia.getPostId(), ignored -> new ArrayList<>())
                    .add(toAttachmentResponse(mediaFile, coverMediaFile));
        }
        return result;
    }

    private Map<Long, MediaFile> ownAttachableMedia(Long userId, List<Long> mediaFileIds) {
        return mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFile>()
                        .eq(MediaFile::getUploaderId, userId)
                        .in(MediaFile::getId, mediaFileIds)
                        .in(MediaFile::getStatus, List.of(
                                STATUS_UPLOADED,
                                STATUS_ATTACHED,
                                STATUS_PENDING_REVIEW,
                                STATUS_APPROVED
                        )))
                .stream()
                .collect(Collectors.toMap(MediaFile::getId, Function.identity()));
    }

    private MediaFile resolveVideoCover(Long userId, Long videoCoverMediaFileId, long videoCount) {
        if (videoCount == 0) {
            if (videoCoverMediaFileId != null) {
                throw new BusinessException(400, "只有视频需要封面");
            }
            return null;
        }
        if (videoCoverMediaFileId == null || videoCoverMediaFileId <= 0) {
            throw new BusinessException(400, "视频需要封面");
        }
        MediaFile coverMedia = mediaFileMapper.selectOne(new LambdaQueryWrapper<MediaFile>()
                .eq(MediaFile::getId, videoCoverMediaFileId)
                .eq(MediaFile::getUploaderId, userId)
                .eq(MediaFile::getFileType, FILE_TYPE_IMAGE)
                .in(MediaFile::getStatus, List.of(STATUS_UPLOADED, STATUS_ATTACHED)));
        if (coverMedia == null) {
            throw new BusinessException(400, "视频封面不存在或还没有上传完成");
        }
        return coverMedia;
    }

    private List<Long> normalizeMediaIds(List<Long> mediaFileIds) {
        LinkedHashSet<Long> distinctIds = mediaFileIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctIds.size() > MAX_POST_MEDIA_COUNT) {
            throw new BusinessException(400, "一条动态最多上传 9 个媒体文件");
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

    private MediaFile requireVideoReviewMedia(Long mediaFileId) {
        MediaFile mediaFile = mediaFileMapper.selectById(mediaFileId);
        if (mediaFile == null || !FILE_TYPE_VIDEO.equals(mediaFile.getFileType())) {
            throw new BusinessException(404, "视频不存在");
        }
        if (!STATUS_PENDING_REVIEW.equals(mediaFile.getStatus())) {
            throw new BusinessException(409, "视频状态已变化");
        }
        return mediaFile;
    }

    private void requireRuruAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !RURU_ADMIN_USERNAME.equals(user.getUsername())) {
            throw new BusinessException(403, "Only ruru can review videos");
        }
        communityMemberService.requireActiveDefaultMember(userId);
    }

    private void updateReviewStatus(Long mediaFileId, String status, LocalDateTime now) {
        int updated = mediaFileMapper.update(null, new LambdaUpdateWrapper<MediaFile>()
                .eq(MediaFile::getId, mediaFileId)
                .eq(MediaFile::getStatus, STATUS_PENDING_REVIEW)
                .set(MediaFile::getStatus, status)
                .set(MediaFile::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(409, "视频状态已变化");
        }
    }

    private void requireR2Configured() {
        if (!r2StorageProperties.isConfigured()) {
            throw new BusinessException(503, "媒体存储还没有配置");
        }
    }

    private String resolveFileType(String contentType) {
        if (SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            return FILE_TYPE_IMAGE;
        }
        if (SUPPORTED_VIDEO_TYPES.contains(contentType)) {
            return FILE_TYPE_VIDEO;
        }
        throw new BusinessException(400, "目前只支持 JPG、PNG、WebP、GIF 图片和 MP4、WebM 视频");
    }

    private void validateUpload(String fileType, String contentType, Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException(400, "文件大小不正确");
        }
        if (FILE_TYPE_IMAGE.equals(fileType) && fileSize > r2StorageProperties.getMaxImageBytes()) {
            throw new BusinessException(400, "图片不能超过 10MB");
        }
        if (FILE_TYPE_VIDEO.equals(fileType) && fileSize > r2StorageProperties.getMaxVideoBytes()) {
            throw new BusinessException(400, "视频不能超过 200MB");
        }
        if (FILE_TYPE_IMAGE.equals(fileType) && !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
            throw new BusinessException(400, "目前只支持 JPG、PNG、WebP、GIF 图片");
        }
        if (FILE_TYPE_VIDEO.equals(fileType) && !SUPPORTED_VIDEO_TYPES.contains(contentType)) {
            throw new BusinessException(400, "目前只支持 MP4、WebM 视频");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private long maxBytes(String fileType) {
        return FILE_TYPE_VIDEO.equals(fileType)
                ? r2StorageProperties.getMaxVideoBytes()
                : r2StorageProperties.getMaxImageBytes();
    }

    private String buildObjectKey(Long userId, String filename, String fileType, String contentType, LocalDateTime now) {
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String directory = FILE_TYPE_VIDEO.equals(fileType) ? "pending/videos" : "images";
        return "community/%s/%d/%s/%s.%s".formatted(
                directory,
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
            if (Set.of("jpg", "jpeg", "png", "webp", "gif", "mp4", "webm").contains(ext)) {
                return ext;
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            default -> "bin";
        };
    }

    private boolean isPubliclyVisible(MediaFile mediaFile) {
        if (FILE_TYPE_IMAGE.equals(mediaFile.getFileType())) {
            return STATUS_UPLOADED.equals(mediaFile.getStatus()) || STATUS_ATTACHED.equals(mediaFile.getStatus());
        }
        return FILE_TYPE_VIDEO.equals(mediaFile.getFileType()) && STATUS_APPROVED.equals(mediaFile.getStatus());
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

    private MediaAttachmentResponse toAttachmentResponse(MediaFile mediaFile, MediaFile coverMediaFile) {
        return new MediaAttachmentResponse(
                mediaFile.getId(),
                mediaFile.getFileType(),
                mediaFile.getContentType(),
                mediaFile.getOriginalFilename(),
                mediaFile.getFileSize(),
                presignGetUrl(mediaFile),
                coverMediaFile == null || !isPubliclyVisible(coverMediaFile) ? null : presignGetUrl(coverMediaFile)
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
