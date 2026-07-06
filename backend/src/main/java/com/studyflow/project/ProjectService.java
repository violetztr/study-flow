package com.studyflow.project;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.project.dto.ProjectRequest;
import com.studyflow.project.dto.ProjectResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectMapper projectMapper) {
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ProjectResponse createProject(Long userId, ProjectRequest request) {
        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(normalizeStatus(request.status()));
        projectMapper.insert(project);
        return ProjectResponse.from(project);
    }

    public List<ProjectResponse> listProjects(Long userId) {
        return projectMapper.selectList(new LambdaQueryWrapper<Project>()
                        .eq(Project::getUserId, userId)
                        .orderByDesc(Project::getId))
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional
    public ProjectResponse updateProject(Long userId, Long projectId, ProjectRequest request) {
        Project project = requireOwnedProject(userId, projectId);
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(normalizeStatus(request.status()));
        projectMapper.updateById(project);
        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        Project project = requireOwnedProject(userId, projectId);
        projectMapper.deleteById(project.getId());
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

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        if (!status.equals("ACTIVE") && !status.equals("ARCHIVED")) {
            throw new BusinessException(400, "项目状态不正确");
        }
        return status;
    }
}
