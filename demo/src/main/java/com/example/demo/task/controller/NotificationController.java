package com.example.demo.task.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.security.entity.User;
import com.example.demo.task.dto.NotificationResponseDTO;
import com.example.demo.task.entity.Notification;
import com.example.demo.task.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponseDTO>>> getNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<NotificationResponseDTO> page = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notifications retrieved successfully", page));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationRepository.countByUserAndIsReadFalse(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread count retrieved successfully", count));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read"));
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationRepository.markAllAsRead(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as read"));
    }

    private NotificationResponseDTO toDTO(Notification n) {
        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setType(n.getType().name());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
