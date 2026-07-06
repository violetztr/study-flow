package com.studyflow.task;

import com.studyflow.common.ApiResponse;
import com.studyflow.security.UserPrincipal;
import com.studyflow.task.dto.TaskRequest;
import com.studyflow.task.dto.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<List<TaskResponse>> listTasks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(taskService.listTasks(
                principal.userId(),
                projectId,
                status,
                priority,
                keyword
        ));
    }

    @PostMapping
    public ApiResponse<TaskResponse> createTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TaskRequest request
    ) {
        return ApiResponse.success(taskService.createTask(principal.userId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TaskResponse> updateTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        return ApiResponse.success(taskService.updateTask(principal.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTask(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        taskService.deleteTask(principal.userId(), id);
        return ApiResponse.success();
    }
}
