package com.example.demo.task.service;

import com.example.demo.task.entity.Notification;
import com.example.demo.task.entity.NotificationType;
import com.example.demo.task.entity.Task;
import com.example.demo.task.repository.NotificationRepository;
import com.example.demo.task.repository.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReminderScheduler {

    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;

    public ReminderScheduler(TaskRepository taskRepository, NotificationRepository notificationRepository) {
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
    }

    // Run every day at 9:00 AM.
    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void scanAndGenerateReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Task> tasks = taskRepository.findActiveTasksWithUserDueBeforeOrOn(tomorrow);

        for (Task task : tasks) {
            LocalDate dueDate = task.getDueDate();
            if (dueDate.isBefore(today)) {
                String msg = "Task is overdue: " + task.getTitle() + " (Due: " + dueDate + ")";
                createUniqueNotification(task, msg, NotificationType.OVERDUE);
            } else if (dueDate.equals(today) || dueDate.equals(tomorrow)) {
                String msg = "Task is due soon: " + task.getTitle() + " (Due: " + dueDate + ")";
                createUniqueNotification(task, msg, NotificationType.DUE_SOON);
            }
        }
    }

    private void createUniqueNotification(Task task, String message, NotificationType type) {
        boolean alreadyNotified = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(task.getUser()).stream()
                .anyMatch(n -> n.getMessage().equals(message));

        if (!alreadyNotified) {
            Notification notification = new Notification();
            notification.setUser(task.getUser());
            notification.setMessage(message);
            notification.setType(type);
            notification.setRead(false);
            notificationRepository.save(notification);
        }
    }
}
