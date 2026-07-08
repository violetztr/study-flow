package com.studyflow.statistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.statistics.dto.StatisticsOverviewResponse;
import com.studyflow.task.Task;
import com.studyflow.task.TaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StatisticsService {
    private final TaskMapper taskMapper;

    public StatisticsService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public StatisticsOverviewResponse overview(Long userId) {
        long totalTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId));
        long completedTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, "DONE"));
        long inProgressTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, "IN_PROGRESS"));
        long overdueTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .ne(Task::getStatus, "DONE")
                .isNotNull(Task::getDeadline)
                .lt(Task::getDeadline, LocalDateTime.now()));
        long totalEstimatedMinutes = sumEstimatedMinutes(userId, null);
        long completedEstimatedMinutes = sumEstimatedMinutes(userId, "DONE");

        return new StatisticsOverviewResponse(
                totalTasks,
                completedTasks,
                inProgressTasks,
                overdueTasks,
                totalEstimatedMinutes,
                completedEstimatedMinutes
        );
    }

    private long sumEstimatedMinutes(Long userId, String status) {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .eq(status != null, Task::getStatus, status)
                        .isNotNull(Task::getEstimatedMinutes))
                .stream()
                .map(Task::getEstimatedMinutes)
                .mapToLong(Integer::longValue)
                .sum();
    }
}
