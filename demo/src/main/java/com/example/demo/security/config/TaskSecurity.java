package com.example.demo.security.config;

import com.example.demo.task.repository.TaskRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("taskSecurity")
public class TaskSecurity {

    private final TaskRepository taskRepository;

    public TaskSecurity(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public boolean isOwnerOrAdmin(Authentication authentication, Long taskId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }

        String currentUsername = authentication.getName();
        return taskRepository.findWithUserById(taskId)
                .map(task -> task.getUser() != null && task.getUser().getUsername().equals(currentUsername))
                .orElse(false);
    }
}
