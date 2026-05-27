package com.example.demo.task.dto;

import com.example.demo.common.validation.EnumValue;
import com.example.demo.task.entity.TaskStatus;
import com.example.demo.task.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Status is required")
    @NotBlank(message = "Status cannot be blank")
    @EnumValue(enumClass = TaskStatus.class, message = "Status must be one of: PENDING, IN_PROGRESS, COMPLETED")
    private String status;

    @NotNull(message = "Priority is required")
    @NotBlank(message = "Priority cannot be blank")
    @EnumValue(enumClass = TaskPriority.class, message = "Priority must be one of: LOW, MEDIUM, HIGH")
    private String priority;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    private LocalDate dueDate;

    // Getters and Setters

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
