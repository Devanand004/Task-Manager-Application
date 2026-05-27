package com.example.demo.task.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.security.entity.User;
import com.example.demo.task.dto.TaskRequestDTO;
import com.example.demo.task.dto.TaskResponseDTO;
import com.example.demo.task.dto.UserProfileResponseDTO;
import com.example.demo.task.entity.TaskStatus;
import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TaskResponseDTO>>> getTasks(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        
        // Users can only see their own tasks, Admins see all tasks if they don't supply a user scoping
        Long scopeUserId = user.getId();
        if (user.getRole().name().equals("ROLE_ADMIN")) {
            scopeUserId = null; // Admin bypass
        }
        
        Page<TaskResponseDTO> tasksPage = taskService.getTasks(scopeUserId, status, priority, category, archived, dueDate, startDate, endDate, search, pageable);
        ApiResponse<Page<TaskResponseDTO>> response = new ApiResponse<>(true, "Tasks retrieved successfully", tasksPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getStats(@AuthenticationPrincipal User user) {
        UserProfileResponseDTO stats = taskService.getUserStats(user);
        ApiResponse<UserProfileResponseDTO> response = new ApiResponse<>(true, "Stats retrieved successfully", stats);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwnerOrAdmin(authentication, #id)")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> getTaskById(@PathVariable Long id) {
        TaskResponseDTO task = taskService.getTaskById(id);
        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(true, "Task retrieved successfully", task);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> createTask(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TaskRequestDTO taskDTO) {
        TaskResponseDTO createdTask = taskService.createTask(taskDTO, user);
        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(true, "Task created successfully", createdTask);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwnerOrAdmin(authentication, #id)")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> updateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TaskRequestDTO taskDTO) {
        TaskResponseDTO updatedTask = taskService.updateTask(id, taskDTO, user);
        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(true, "Task updated successfully", updatedTask);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwnerOrAdmin(authentication, #id)")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> archiveTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        TaskResponseDTO updatedTask = taskService.archiveTask(id, user);
        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(true, "Task archived successfully", updatedTask);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwnerOrAdmin(authentication, #id)")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> restoreTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        TaskResponseDTO updatedTask = taskService.restoreTask(id, user);
        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(true, "Task restored successfully", updatedTask);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @taskSecurity.isOwnerOrAdmin(authentication, #id)")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        taskService.deleteTask(id, user);
        ApiResponse<Void> response = new ApiResponse<>(true, "Task deleted successfully");
        return ResponseEntity.ok(response);
    }
}
