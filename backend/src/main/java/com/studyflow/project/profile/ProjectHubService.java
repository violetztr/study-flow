package com.studyflow.project.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.project.Project;
import com.studyflow.project.ProjectMapper;
import com.studyflow.project.profile.dto.ProjectProfileRequest;
import com.studyflow.project.profile.dto.ProjectProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectHubService {
    private final ProjectMapper projectMapper;
    private final ProjectProfileMapper projectProfileMapper;

    public ProjectHubService(
            ProjectMapper projectMapper,
            ProjectProfileMapper projectProfileMapper
    ) {
        this.projectMapper = projectMapper;
        this.projectProfileMapper = projectProfileMapper;
    }

    public ProjectProfileResponse getProfile(Long userId, Long projectId) {
        requireOwnedProject(userId, projectId);
        ProjectProfile profile = findProfile(projectId);
        if (profile == null) {
            return ProjectProfileResponse.empty(projectId);
        }
        return ProjectProfileResponse.from(profile);
    }

    @Transactional
    public ProjectProfileResponse upsertProfile(
            Long userId,
            Long projectId,
            ProjectProfileRequest request
    ) {
        requireOwnedProject(userId, projectId);
        ProjectProfile profile = findProfile(projectId);
        if (profile == null) {
            profile = new ProjectProfile();
            profile.setProjectId(projectId);
            profile.setUserId(userId);
            applyRequest(profile, request);
            projectProfileMapper.insert(profile);
            return ProjectProfileResponse.from(profile);
        }

        applyRequest(profile, request);
        projectProfileMapper.updateById(profile);
        return ProjectProfileResponse.from(profile);
    }

    private Project requireOwnedProject(Long userId, Long projectId) {
        Project project = projectMapper.selectOne(new LambdaQueryWrapper<Project>()
                .eq(Project::getId, projectId)
                .eq(Project::getUserId, userId));
        if (project == null) {
            throw new BusinessException(404, "项目不存在");
        }
        return project;
    }

    private ProjectProfile findProfile(Long projectId) {
        return projectProfileMapper.selectOne(new LambdaQueryWrapper<ProjectProfile>()
                .eq(ProjectProfile::getProjectId, projectId));
    }

    private void applyRequest(ProjectProfile profile, ProjectProfileRequest request) {
        profile.setHeadline(request.headline());
        profile.setProductionUrl(request.productionUrl());
        profile.setApiDocUrl(request.apiDocUrl());
        profile.setDatabaseDocUrl(request.databaseDocUrl());
        profile.setArchitectureSummary(request.architectureSummary());
        profile.setInterviewHighlights(request.interviewHighlights());
        profile.setCoverImageUrl(request.coverImageUrl());
    }
}
