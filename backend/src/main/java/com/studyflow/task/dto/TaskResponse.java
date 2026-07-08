package com.studyflow.task.dto;

import com.studyflow.task.Task;

import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        String status,
        String priority,
        LocalDateTime deadline,
        Integer estimatedMinutes,
        LocalDateTime completedAt,
        List<Long> tagIds
) {
    public static TaskResponse from(Task task, List<Long> tagIds) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                task.getEstimatedMinutes(),
                task.getCompletedAt(),
                tagIds
        );
    }
}
