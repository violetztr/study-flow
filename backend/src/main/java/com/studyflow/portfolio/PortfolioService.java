package com.studyflow.portfolio;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.github.GitHubRepository;
import com.studyflow.github.GitHubRepositoryMapper;
import com.studyflow.github.dto.GitHubRepositoryResponse;
import com.studyflow.portfolio.dto.PublicPortfolioProjectResponse;
import com.studyflow.project.Project;
import com.studyflow.project.ProjectMapper;
import com.studyflow.project.profile.PortfolioProject;
import com.studyflow.project.profile.PortfolioProjectMapper;
import com.studyflow.project.profile.ProjectProfile;
import com.studyflow.project.profile.ProjectProfileMapper;
import com.studyflow.project.profile.ProjectTechStack;
import com.studyflow.project.profile.ProjectTechStackMapper;
import com.studyflow.project.profile.dto.ProjectTechStackResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioService {
    private final PortfolioProjectMapper portfolioProjectMapper;
    private final ProjectMapper projectMapper;
    private final ProjectProfileMapper projectProfileMapper;
    private final ProjectTechStackMapper projectTechStackMapper;
    private final GitHubRepositoryMapper gitHubRepositoryMapper;

    public PortfolioService(
            PortfolioProjectMapper portfolioProjectMapper,
            ProjectMapper projectMapper,
            ProjectProfileMapper projectProfileMapper,
            ProjectTechStackMapper projectTechStackMapper,
            GitHubRepositoryMapper gitHubRepositoryMapper
    ) {
        this.portfolioProjectMapper = portfolioProjectMapper;
        this.projectMapper = projectMapper;
        this.projectProfileMapper = projectProfileMapper;
        this.projectTechStackMapper = projectTechStackMapper;
        this.gitHubRepositoryMapper = gitHubRepositoryMapper;
    }

    public List<PublicPortfolioProjectResponse> listPublicProjects() {
        return portfolioProjectMapper.selectList(new LambdaQueryWrapper<PortfolioProject>()
                        .eq(PortfolioProject::getPublicVisible, true)
                        .orderByDesc(PortfolioProject::getFeatured)
                        .orderByAsc(PortfolioProject::getDisplayOrder)
                        .orderByDesc(PortfolioProject::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PublicPortfolioProjectResponse getPublicProject(String slug) {
        PortfolioProject portfolioProject = portfolioProjectMapper.selectOne(new LambdaQueryWrapper<PortfolioProject>()
                .eq(PortfolioProject::getSlug, slug)
                .eq(PortfolioProject::getPublicVisible, true));
        if (portfolioProject == null) {
            throw new BusinessException(404, "Public project does not exist");
        }
        return toResponse(portfolioProject);
    }

    private PublicPortfolioProjectResponse toResponse(PortfolioProject portfolioProject) {
        Project project = projectMapper.selectById(portfolioProject.getProjectId());
        if (project == null) {
            throw new BusinessException(404, "Project does not exist");
        }

        ProjectProfile profile = projectProfileMapper.selectOne(new LambdaQueryWrapper<ProjectProfile>()
                .eq(ProjectProfile::getProjectId, portfolioProject.getProjectId()));
        List<ProjectTechStackResponse> techStacks = projectTechStackMapper.selectList(new LambdaQueryWrapper<ProjectTechStack>()
                        .eq(ProjectTechStack::getProjectId, portfolioProject.getProjectId())
                        .orderByAsc(ProjectTechStack::getSortOrder)
                        .orderByAsc(ProjectTechStack::getId))
                .stream()
                .map(ProjectTechStackResponse::from)
                .toList();
        GitHubRepository githubRepository = gitHubRepositoryMapper.selectOne(new LambdaQueryWrapper<GitHubRepository>()
                .eq(GitHubRepository::getProjectId, portfolioProject.getProjectId()));
        GitHubRepositoryResponse githubResponse = githubRepository == null
                ? null
                : GitHubRepositoryResponse.from(githubRepository);

        return PublicPortfolioProjectResponse.from(project, portfolioProject, profile, techStacks, githubResponse);
    }
}
