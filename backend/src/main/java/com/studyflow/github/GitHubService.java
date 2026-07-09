package com.studyflow.github;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.github.dto.GitHubRepositoryRequest;
import com.studyflow.github.dto.GitHubRepositoryResponse;
import com.studyflow.github.dto.GitHubSyncResult;
import com.studyflow.project.Project;
import com.studyflow.project.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class GitHubService {
    private final ProjectMapper projectMapper;
    private final GitHubRepositoryMapper gitHubRepositoryMapper;
    private final GitHubClient gitHubClient;

    public GitHubService(
            ProjectMapper projectMapper,
            GitHubRepositoryMapper gitHubRepositoryMapper,
            GitHubClient gitHubClient
    ) {
        this.projectMapper = projectMapper;
        this.gitHubRepositoryMapper = gitHubRepositoryMapper;
        this.gitHubClient = gitHubClient;
    }

    @Transactional
    public GitHubRepositoryResponse upsertRepository(
            Long userId,
            Long projectId,
            GitHubRepositoryRequest request
    ) {
        requireOwnedProject(userId, projectId);
        String owner = request.owner().trim();
        String repo = request.repo().trim();
        GitHubRepository repository = findByProjectId(projectId);

        if (repository == null) {
            repository = new GitHubRepository();
            repository.setProjectId(projectId);
            repository.setUserId(userId);
            repository.setStars(0);
            repository.setForks(0);
            repository.setOpenIssues(0);
            repository.setReadmePresent(false);
            applyRepositoryRequest(repository, owner, repo);
            gitHubRepositoryMapper.insert(repository);
            return GitHubRepositoryResponse.from(repository);
        }

        boolean repositoryChanged = !owner.equals(repository.getOwner()) || !repo.equals(repository.getRepo());
        applyRepositoryRequest(repository, owner, repo);
        if (repositoryChanged) {
            clearSyncedMetadata(repository);
        }
        gitHubRepositoryMapper.updateById(repository);
        return GitHubRepositoryResponse.from(repository);
    }

    @Transactional
    public GitHubRepositoryResponse syncRepository(Long userId, Long projectId) {
        requireOwnedProject(userId, projectId);
        GitHubRepository repository = findByProjectId(projectId);
        if (repository == null) {
            throw new BusinessException(404, "GitHub repository settings do not exist");
        }

        GitHubSyncResult result = gitHubClient.fetchRepository(repository.getOwner(), repository.getRepo());
        applySyncResult(repository, result);
        gitHubRepositoryMapper.updateById(repository);
        return GitHubRepositoryResponse.from(repository);
    }

    private Project requireOwnedProject(Long userId, Long projectId) {
        Project project = projectMapper.selectOne(new LambdaQueryWrapper<Project>()
                .eq(Project::getId, projectId)
                .eq(Project::getUserId, userId));
        if (project == null) {
            throw new BusinessException(404, "Project does not exist");
        }
        return project;
    }

    private GitHubRepository findByProjectId(Long projectId) {
        return gitHubRepositoryMapper.selectOne(new LambdaQueryWrapper<GitHubRepository>()
                .eq(GitHubRepository::getProjectId, projectId));
    }

    private void applyRepositoryRequest(GitHubRepository repository, String owner, String repo) {
        repository.setOwner(owner);
        repository.setRepo(repo);
    }

    private void clearSyncedMetadata(GitHubRepository repository) {
        repository.setHtmlUrl(null);
        repository.setDescription(null);
        repository.setDefaultBranch(null);
        repository.setPrimaryLanguage(null);
        repository.setStars(0);
        repository.setForks(0);
        repository.setOpenIssues(0);
        repository.setPushedAt(null);
        repository.setLastSyncedAt(null);
        repository.setReadmePresent(false);
        repository.setLanguagesJson(null);
        repository.setLatestCommitsJson(null);
    }

    private void applySyncResult(GitHubRepository repository, GitHubSyncResult result) {
        repository.setHtmlUrl(result.htmlUrl());
        repository.setDescription(result.description());
        repository.setDefaultBranch(result.defaultBranch());
        repository.setPrimaryLanguage(result.primaryLanguage());
        repository.setStars(result.stars());
        repository.setForks(result.forks());
        repository.setOpenIssues(result.openIssues());
        repository.setPushedAt(result.pushedAt());
        repository.setLastSyncedAt(LocalDateTime.now());
        repository.setReadmePresent(Boolean.TRUE.equals(result.readmePresent()));
        repository.setLanguagesJson(result.languagesJson());
        repository.setLatestCommitsJson(result.latestCommitsJson());
    }
}
