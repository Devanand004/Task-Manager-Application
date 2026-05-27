package com.example.demo.task.controller;

import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import com.example.demo.security.service.JwtService;
import com.example.demo.task.entity.Notification;
import com.example.demo.task.entity.NotificationType;
import com.example.demo.task.repository.NotificationRepository;
import com.example.demo.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController.
 *
 * WHY this matters:
 *   Notifications are user-scoped. We must ensure:
 *   - Users only see their own notifications (not other users')
 *   - Mark-as-read rejects cross-user access (ownership enforcement)
 *   - Unread count is accurate
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("NotificationController Integration Tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private User otherUser;
    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("notifyuser");
        testUser.setPassword(passwordEncoder.encode("pass"));
        testUser.setRole(Role.ROLE_USER);
        userRepository.save(testUser);
        userToken = "Bearer " + jwtService.generateToken(testUser);

        otherUser = new User();
        otherUser.setUsername("othernotifyuser");
        otherUser.setPassword(passwordEncoder.encode("pass"));
        otherUser.setRole(Role.ROLE_USER);
        userRepository.save(otherUser);
        otherUserToken = "Bearer " + jwtService.generateToken(otherUser);
    }

    /** Helper to create a notification for a user */
    private Notification createNotificationFor(User owner, String message, boolean read) {
        Notification n = new Notification();
        n.setUser(owner);
        n.setMessage(message);
        n.setType(NotificationType.DUE_SOON);
        n.setRead(read);
        return notificationRepository.save(n);
    }

    // ============================================================
    // GET /api/v1/notifications
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotifications {

        @Test
        @DisplayName("Returns 200 with user's notifications (not other user's)")
        void getNotifications_ReturnsOnlyOwnNotifications() throws Exception {
            createNotificationFor(testUser, "Your task is due soon", false);
            createNotificationFor(otherUser, "Other user's notification", false);

            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].message").value("Your task is due soon"))
                    .andExpect(jsonPath("$.data.content[1]").doesNotExist());
        }

        @Test
        @DisplayName("Returns 403 when no token is provided")
        void getNotifications_NoToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns empty page when user has no notifications")
        void getNotifications_NoNotifications_ReturnsEmptyPage() throws Exception {
            mockMvc.perform(get("/api/v1/notifications")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }
    }

    // ============================================================
    // GET /api/v1/notifications/unread-count
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count")
    class GetUnreadCount {

        @Test
        @DisplayName("Returns correct count of unread notifications")
        void getUnreadCount_ReturnsCorrectCount() throws Exception {
            createNotificationFor(testUser, "Unread notification 1", false);
            createNotificationFor(testUser, "Unread notification 2", false);
            createNotificationFor(testUser, "Already read", true);

            mockMvc.perform(get("/api/v1/notifications/unread-count")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(2));
        }

        @Test
        @DisplayName("Returns 0 when all notifications are read")
        void getUnreadCount_AllRead_ReturnsZero() throws Exception {
            createNotificationFor(testUser, "Already read", true);

            mockMvc.perform(get("/api/v1/notifications/unread-count")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(0));
        }
    }

    // ============================================================
    // PUT /api/v1/notifications/{id}/read
    // ============================================================

    @Nested
    @DisplayName("PUT /api/v1/notifications/{id}/read")
    class MarkAsRead {

        @Test
        @DisplayName("Returns 200 when user marks their own notification as read")
        void markAsRead_OwnNotification_ReturnsOk() throws Exception {
            Notification n = createNotificationFor(testUser, "My notification", false);

            mockMvc.perform(put("/api/v1/notifications/" + n.getId() + "/read")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Returns 403 when user tries to mark another user's notification as read")
        void markAsRead_OtherUserNotification_ReturnsForbidden() throws Exception {
            Notification n = createNotificationFor(otherUser, "Other user's notification", false);

            mockMvc.perform(put("/api/v1/notifications/" + n.getId() + "/read")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // POST /api/v1/notifications/mark-all-read
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/notifications/mark-all-read")
    class MarkAllAsRead {

        @Test
        @DisplayName("Returns 200 when marking all notifications as read")
        void markAllAsRead_ReturnsOk() throws Exception {
            createNotificationFor(testUser, "Notification 1", false);
            createNotificationFor(testUser, "Notification 2", false);

            mockMvc.perform(post("/api/v1/notifications/mark-all-read")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
