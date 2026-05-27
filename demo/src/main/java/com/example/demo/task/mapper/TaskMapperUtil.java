package com.example.demo.task.mapper;

import com.example.demo.task.entity.TaskStatus;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class TaskMapperUtil {

    @Named("stringToTaskStatus")
    public TaskStatus stringToTaskStatus(String status) {
        if (status == null || status.isEmpty()) {
            return TaskStatus.PENDING;
        }
        try {
            return TaskStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TaskStatus.PENDING;
        }
    }

    @Named("stringToTaskPriority")
    public com.example.demo.task.entity.TaskPriority stringToTaskPriority(String priority) {
        if (priority == null || priority.isEmpty()) {
            return com.example.demo.task.entity.TaskPriority.MEDIUM;
        }
        try {
            return com.example.demo.task.entity.TaskPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return com.example.demo.task.entity.TaskPriority.MEDIUM;
        }
    }
}
