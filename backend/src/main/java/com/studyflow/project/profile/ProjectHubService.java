package com.studyflow.project.profile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.project.Project;
import com.studyflow.project.ProjectMapper;
import com.studyflow.project.profile.dto.PortfolioProjectRequest;
import com.studyflow.project.profile.dto.PortfolioProjectResponse;
import com.studyflow.project.profile.dto.ProjectProfileRequest;
import com.studyflow.project.profile.dto.ProjectProfileResponse;
import com.studyflow.project.profile.dto.ProjectTechStackRequest;
import com.studyflow.project.profile.dto.ProjectTechStackResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProjectHubService {
    private static final Set<String> TECH_STACK_CATEGORIES = Set.of(
            "FRONTEND",
            "BACKEND",
            "DATABASE",
            "DEPLOYMENT",
            "TOOLING",
            "OTHER"
    );

    private final ProjectMapper projectMapper;
    private final ProjectProfileMapper projectProfileMapper;
    private final ProjectTechStackMapper projectTechStackMapper;
    private final PortfolioProjectMapper portfolioProjectMapper;

    public ProjectHubService(
            ProjectMapper projectMapper,
            ProjectProfileMapper projectProfileMapper,
            ProjectTechStackMapper projectTechStackMapper,
            PortfolioProjectMapper portfolioProjectMapper
    ) {
        this.projectMapper = projectMapper;
        this.projectProfileMapper = projectProfileMapper;
        this.projectTechStackMapper = projectTechStackMapper;
        this.portfolioProjectMapper = portfolioProjectMapper;
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

    @Transactional
    public List<ProjectTechStackResponse> replaceTechStacks(
            Long userId,
            Long projectId,
            List<ProjectTechStackRequest> requests
    ) {
        requireOwnedProject(userId, projectId);
        projectTechStackMapper.delete(new LambdaQueryWrapper<ProjectTechStack>()
                .eq(ProjectTechStack::getProjectId, projectId)
                .eq(ProjectTechStack::getUserId, userId));

        List<ProjectTechStackRequest> safeRequests = requests == null ? List.of() : requests;
        for (int index = 0; index < safeRequests.size(); index++) {
            ProjectTechStackRequest request = safeRequests.get(index);
            ProjectTechStack techStack = new ProjectTechStack();
            techStack.setProjectId(projectId);
            techStack.setUserId(userId);
            techStack.setName(request.name());
            techStack.setCategory(normalizeTechStackCategory(request.category()));
            techStack.setSortOrder(request.sortOrder() == null ? index : request.sortOrder());
            projectTechStackMapper.insert(techStack);
        }

        return listTechStacks(projectId);
    }

    @Transactional
    public PortfolioProjectResponse upsertPortfolioSettings(
            Long userId,
            Long projectId,
            PortfolioProjectRequest request
    ) {
        requireOwnedProject(userId, projectId);
        String slug = request.slug().trim();
        PortfolioProject sameSlug = portfolioProjectMapper.selectOne(new LambdaQueryWrapper<PortfolioProject>()
                .eq(PortfolioProject::getSlug, slug));
        if (sameSlug != null && !sameSlug.getProjectId().equals(projectId)) {
            throw new BusinessException(400, "Portfolio slug already exists");
        }

        PortfolioProject portfolioProject = findPortfolioProject(projectId);
        if (portfolioProject == null) {
            portfolioProject = new PortfolioProject();
            portfolioProject.setProjectId(projectId);
            portfolioProject.setUserId(userId);
            applyPortfolioRequest(portfolioProject, request, slug);
            portfolioProjectMapper.insert(portfolioProject);
            return PortfolioProjectResponse.from(portfolioProject);
        }

        applyPortfolioRequest(portfolioProject, request, slug);
        portfolioProjectMapper.updateById(portfolioProject);
        return PortfolioProjectResponse.from(portfolioProject);
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

    private List<ProjectTechStackResponse> listTechStacks(Long projectId) {
        return projectTechStackMapper.selectList(new LambdaQueryWrapper<ProjectTechStack>()
                        .eq(ProjectTechStack::getProjectId, projectId)
                        .orderByAsc(ProjectTechStack::getSortOrder)
                        .orderByAsc(ProjectTechStack::getId))
                .stream()
                .map(ProjectTechStackResponse::from)
                .toList();
    }

    private PortfolioProject findPortfolioProject(Long projectId) {
        return portfolioProjectMapper.selectOne(new LambdaQueryWrapper<PortfolioProject>()
                .eq(PortfolioProject::getProjectId, projectId));
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

    private String normalizeTechStackCategory(String category) {
        if (category == null || category.isBlank()) {
            return "OTHER";
        }
        String normalizedCategory = category.trim().toUpperCase(Locale.ROOT);
        if (!TECH_STACK_CATEGORIES.contains(normalizedCategory)) {
            throw new BusinessException(400, "Tech stack category is invalid");
        }
        return normalizedCategory;
    }

    private void applyPortfolioRequest(
            PortfolioProject portfolioProject,
            PortfolioProjectRequest request,
            String slug
    ) {
        portfolioProject.setSlug(slug);
        portfolioProject.setPublicVisible(Boolean.TRUE.equals(request.publicVisible()));
        portfolioProject.setFeatured(Boolean.TRUE.equals(request.featured()));
        portfolioProject.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        portfolioProject.setPublicSummary(request.publicSummary());
    }
}
