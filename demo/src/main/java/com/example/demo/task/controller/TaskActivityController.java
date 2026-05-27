package com.example.demo.task.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.security.entity.User;
import com.example.demo.task.dto.TaskActivityResponseDTO;
import com.example.demo.task.entity.TaskActivity;
import com.example.demo.task.repository.TaskActivityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks/activity")
public class TaskActivityController {

    private final TaskActivityRepository taskActivityRepository;

    public TaskActivityController(TaskActivityRepository taskActivityRepository) {
        this.taskActivityRepository = taskActivityRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TaskActivityResponseDTO>>> getUserActivity(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 15) Pageable pageable) {
        Page<TaskActivityResponseDTO> page = taskActivityRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "User activity logs retrieved successfully", page));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwnerOrAdmin(authentication, #taskId)")
    public ResponseEntity<ApiResponse<Page<TaskActivityResponseDTO>>> getTaskActivity(
            @PathVariable Long taskId,
            @PageableDefault(size = 15) Pageable pageable) {
        Page<TaskActivityResponseDTO> page = taskActivityRepository.findByTaskIdOrderByCreatedAtDesc(taskId, pageable)
                .map(this::toDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Task activity logs retrieved successfully", page));
    }

    private TaskActivityResponseDTO toDTO(TaskActivity a) {
        TaskActivityResponseDTO dto = new TaskActivityResponseDTO();
        dto.setId(a.getId());
        dto.setTaskId(a.getTaskId());
        dto.setUsername(a.getUser().getUsername());
        dto.setAction(a.getAction());
        dto.setDetails(a.getDetails());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }
}
