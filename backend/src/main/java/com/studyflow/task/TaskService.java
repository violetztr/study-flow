package com.studyflow.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.common.BusinessException;
import com.studyflow.project.Project;
import com.studyflow.project.ProjectMapper;
import com.studyflow.tag.Tag;
import com.studyflow.tag.TagMapper;
import com.studyflow.task.dto.TaskRequest;
import com.studyflow.task.dto.TaskResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private final TaskMapper taskMapper;
    private final TaskTagMapper taskTagMapper;
    private final ProjectMapper projectMapper;
    private final TagMapper tagMapper;

    public TaskService(
            TaskMapper taskMapper,
            TaskTagMapper taskTagMapper,
            ProjectMapper projectMapper,
            TagMapper tagMapper
    ) {
        this.taskMapper = taskMapper;
        this.taskTagMapper = taskTagMapper;
        this.projectMapper = projectMapper;
        this.tagMapper = tagMapper;
    }

    @Transactional
    public TaskResponse createTask(Long userId, TaskRequest request) {
        requireOwnedProject(userId, request.projectId());
        List<Long> tagIds = normalizeTagIds(request.tagIds());
        requireOwnedTags(userId, tagIds);

        Task task = new Task();
        task.setUserId(userId);
        task.setProjectId(request.projectId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(normalizeStatus(request.status()));
        task.setPriority(normalizePriority(request.priority()));
        task.setDeadline(request.deadline());
        task.setEstimatedMinutes(request.estimatedMinutes());
        task.setCompletedAt(completedAtForStatus(task.getStatus()));
        taskMapper.insert(task);
        replaceTaskTags(task.getId(), tagIds);

        return TaskResponse.from(task, tagIds);
    }

    public List<TaskResponse> listTasks(Long userId, Long projectId, String status, String priority, String keyword) {
        LambdaQueryWrapper<Task> query = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(projectId != null, Task::getProjectId, projectId)
                .eq(status != null && !status.isBlank(), Task::getStatus, status)
                .eq(priority != null && !priority.isBlank(), Task::getPriority, priority)
                .like(keyword != null && !keyword.isBlank(), Task::getTitle, keyword)
                .orderByDesc(Task::getId);

        return taskMapper.selectList(query)
                .stream()
                .map(task -> TaskResponse.from(task, listTagIds(task.getId())))
                .toList();
    }

    @Transactional
    public TaskResponse updateTask(Long userId, Long taskId, TaskRequest request) {
        Task task = requireOwnedTask(userId, taskId);
        requireOwnedProject(userId, request.projectId());
        List<Long> tagIds = normalizeTagIds(request.tagIds());
        requireOwnedTags(userId, tagIds);

        task.setProjectId(request.projectId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(normalizeStatus(request.status()));
        task.setPriority(normalizePriority(request.priority()));
        task.setDeadline(request.deadline());
        task.setEstimatedMinutes(request.estimatedMinutes());
        task.setCompletedAt(completedAtForStatus(task.getStatus()));
        taskMapper.updateById(task);
        replaceTaskTags(task.getId(), tagIds);

        return TaskResponse.from(task, tagIds);
    }

    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = requireOwnedTask(userId, taskId);
        taskTagMapper.delete(new LambdaQueryWrapper<TaskTag>().eq(TaskTag::getTaskId, task.getId()));
        taskMapper.deleteById(task.getId());
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

    private Task requireOwnedTask(Long userId, Long taskId) {
        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>()
                .eq(Task::getId, taskId)
                .eq(Task::getUserId, userId));
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        return task;
    }

    private void requireOwnedTags(Long userId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getId, tagId)
                    .eq(Tag::getUserId, userId));
            if (tag == null) {
                throw new BusinessException(404, "标签不存在");
            }
        }
    }

    private void replaceTaskTags(Long taskId, List<Long> tagIds) {
        taskTagMapper.delete(new LambdaQueryWrapper<TaskTag>().eq(TaskTag::getTaskId, taskId));
        for (Long tagId : tagIds) {
            TaskTag taskTag = new TaskTag();
            taskTag.setTaskId(taskId);
            taskTag.setTagId(tagId);
            taskTagMapper.insert(taskTag);
        }
    }

    private List<Long> listTagIds(Long taskId) {
        return taskTagMapper.selectList(new LambdaQueryWrapper<TaskTag>()
                        .eq(TaskTag::getTaskId, taskId))
                .stream()
                .map(TaskTag::getTagId)
                .toList();
    }

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null) {
            return List.of();
        }
        return tagIds.stream().distinct().toList();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PENDING";
        }
        if (!status.equals("PENDING") && !status.equals("IN_PROGRESS") && !status.equals("DONE")) {
            throw new BusinessException(400, "任务状态不正确");
        }
        return status;
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "MEDIUM";
        }
        if (!priority.equals("LOW") && !priority.equals("MEDIUM") && !priority.equals("HIGH")) {
            throw new BusinessException(400, "任务优先级不正确");
        }
        return priority;
    }

    private LocalDateTime completedAtForStatus(String status) {
        if (status.equals("DONE")) {
            return LocalDateTime.now();
        }
        return null;
    }
}
