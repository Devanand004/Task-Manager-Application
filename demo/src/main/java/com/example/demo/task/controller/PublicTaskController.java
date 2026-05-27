package com.example.demo.task.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.task.dto.TaskResponseDTO;
import com.example.demo.task.service.TaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/tasks")
public class PublicTaskController {

    private final TaskService taskService;

    public PublicTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TaskResponseDTO>>> getAllTasks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TaskResponseDTO> tasksPage = taskService.getTasks(null, null, null, null, false, null, null, null, null, pageable);
        ApiResponse<Page<TaskResponseDTO>> response = new ApiResponse<>(true, "Tasks retrieved successfully", tasksPage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponseDTO>> getTaskById(@PathVariable Long id) {
        TaskResponseDTO task = taskService.getTaskById(id);
        ApiResponse<TaskResponseDTO> response = new ApiResponse<>(true, "Task retrieved successfully", task);
        return ResponseEntity.ok(response);
    }
}
