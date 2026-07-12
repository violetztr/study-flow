package com.studyflow.media;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("media_files")
public class MediaFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uploaderId;
    private String storageProvider;
    private String bucketName;
    private String objectKey;
    private String originalFilename;
    private String contentType;
    private String fileType;
    private Long fileSize;
    private String status;
    private String transcodeStatus;
    private String transcodeError;
    private LocalDateTime transcodeStartedAt;
    private LocalDateTime transcodeCompletedAt;
    private String hlsMasterObjectKey;
    private Integer durationSeconds;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(Long uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(String storageProvider) {
        this.storageProvider = storageProvider;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTranscodeStatus() {
        return transcodeStatus;
    }

    public void setTranscodeStatus(String transcodeStatus) {
        this.transcodeStatus = transcodeStatus;
    }

    public String getTranscodeError() {
        return transcodeError;
    }

    public void setTranscodeError(String transcodeError) {
        this.transcodeError = transcodeError;
    }

    public LocalDateTime getTranscodeStartedAt() {
        return transcodeStartedAt;
    }

    public void setTranscodeStartedAt(LocalDateTime transcodeStartedAt) {
        this.transcodeStartedAt = transcodeStartedAt;
    }

    public LocalDateTime getTranscodeCompletedAt() {
        return transcodeCompletedAt;
    }

    public void setTranscodeCompletedAt(LocalDateTime transcodeCompletedAt) {
        this.transcodeCompletedAt = transcodeCompletedAt;
    }

    public String getHlsMasterObjectKey() {
        return hlsMasterObjectKey;
    }

    public void setHlsMasterObjectKey(String hlsMasterObjectKey) {
        this.hlsMasterObjectKey = hlsMasterObjectKey;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
