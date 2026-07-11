package com.studyflow.community.view;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("community_post_views")
public class CommunityPostView {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long circleId;
    private Long postId;
    private Long userId;
    private String viewerKey;
    private Integer maxProgressSeconds;
    private Integer durationSeconds;
    private Boolean counted;
    private LocalDateTime firstViewedAt;
    private LocalDateTime lastViewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCircleId() {
        return circleId;
    }

    public void setCircleId(Long circleId) {
        this.circleId = circleId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getViewerKey() {
        return viewerKey;
    }

    public void setViewerKey(String viewerKey) {
        this.viewerKey = viewerKey;
    }

    public Integer getMaxProgressSeconds() {
        return maxProgressSeconds;
    }

    public void setMaxProgressSeconds(Integer maxProgressSeconds) {
        this.maxProgressSeconds = maxProgressSeconds;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Boolean getCounted() {
        return counted;
    }

    public void setCounted(Boolean counted) {
        this.counted = counted;
    }

    public LocalDateTime getFirstViewedAt() {
        return firstViewedAt;
    }

    public void setFirstViewedAt(LocalDateTime firstViewedAt) {
        this.firstViewedAt = firstViewedAt;
    }

    public LocalDateTime getLastViewedAt() {
        return lastViewedAt;
    }

    public void setLastViewedAt(LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
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
