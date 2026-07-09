package com.studyflow.project.profile;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("project_profiles")
public class ProjectProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private String headline;
    private String productionUrl;
    private String apiDocUrl;
    private String databaseDocUrl;
    private String architectureSummary;
    private String interviewHighlights;
    private String coverImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getProductionUrl() {
        return productionUrl;
    }

    public void setProductionUrl(String productionUrl) {
        this.productionUrl = productionUrl;
    }

    public String getApiDocUrl() {
        return apiDocUrl;
    }

    public void setApiDocUrl(String apiDocUrl) {
        this.apiDocUrl = apiDocUrl;
    }

    public String getDatabaseDocUrl() {
        return databaseDocUrl;
    }

    public void setDatabaseDocUrl(String databaseDocUrl) {
        this.databaseDocUrl = databaseDocUrl;
    }

    public String getArchitectureSummary() {
        return architectureSummary;
    }

    public void setArchitectureSummary(String architectureSummary) {
        this.architectureSummary = architectureSummary;
    }

    public String getInterviewHighlights() {
        return interviewHighlights;
    }

    public void setInterviewHighlights(String interviewHighlights) {
        this.interviewHighlights = interviewHighlights;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
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
