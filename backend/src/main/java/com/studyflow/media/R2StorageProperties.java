package com.studyflow.media;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "study-flow.media.r2")
public class R2StorageProperties {
    private String accountId;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucket = "ruru-community";
    private Duration uploadUrlTtl = Duration.ofMinutes(10);
    private Duration readUrlTtl = Duration.ofHours(1);
    private long maxImageBytes = 10 * 1024 * 1024;
    private long maxVideoBytes = 50 * 1024 * 1024;

    public boolean isConfigured() {
        return StringUtils.hasText(accountId)
                && StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(secretAccessKey)
                && StringUtils.hasText(bucket);
    }

    public String endpoint() {
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public Duration getUploadUrlTtl() {
        return uploadUrlTtl;
    }

    public void setUploadUrlTtl(Duration uploadUrlTtl) {
        this.uploadUrlTtl = uploadUrlTtl;
    }

    public Duration getReadUrlTtl() {
        return readUrlTtl;
    }

    public void setReadUrlTtl(Duration readUrlTtl) {
        this.readUrlTtl = readUrlTtl;
    }

    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    public void setMaxImageBytes(long maxImageBytes) {
        this.maxImageBytes = maxImageBytes;
    }

    public long getMaxVideoBytes() {
        return maxVideoBytes;
    }

    public void setMaxVideoBytes(long maxVideoBytes) {
        this.maxVideoBytes = maxVideoBytes;
    }
}
