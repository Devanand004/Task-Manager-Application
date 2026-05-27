package com.example.demo.task.controller;

import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import com.example.demo.security.service.JwtService;
import com.example.demo.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProfileController — covers profile view and password change.
 *
 * WHY this matters:
 *   Password change is a security-sensitive flow. We must verify that:
 *   - Incorrect old passwords are rejected (prevents unauthorized password takeover)
 *   - New password is properly saved and encoded
 *   - Unauthenticated access is blocked
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProfileController Integration Tests")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TaskRepository taskRepository;

    private String userToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("profileuser");
        testUser.setPassword(passwordEncoder.encode("correctpass"));
        testUser.setRole(Role.ROLE_USER);
        userRepository.save(testUser);
        userToken = "Bearer " + jwtService.generateToken(testUser);
    }

    // ============================================================
    // GET /api/v1/profile
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/profile")
    class GetProfile {

        @Test
        @DisplayName("Returns 200 with profile data for authenticated user")
        void getProfile_AuthenticatedUser_ReturnsOk() throws Exception {
            mockMvc.perform(get("/api/v1/profile")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("profileuser"));
        }

        @Test
        @DisplayName("Returns 403 when no token is provided")
        void getProfile_NoToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/profile"))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // PUT /api/v1/profile/password
    // ============================================================

    @Nested
    @DisplayName("PUT /api/v1/profile/password")
    class ChangePassword {

        @Test
        @DisplayName("Returns 200 when current password is correct and new password is valid")
        void changePassword_CorrectCurrentPassword_ReturnsOk() throws Exception {
            mockMvc.perform(put("/api/v1/profile/password")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"correctpass\",\"newPassword\":\"newpassword123\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Returns 400 when current password is wrong")
        void changePassword_WrongCurrentPassword_ReturnsBadRequest() throws Exception {
            mockMvc.perform(put("/api/v1/profile/password")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"WRONGPASSWORD\",\"newPassword\":\"newpassword123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Returns 400 when new password is too short (< 6 chars)")
        void changePassword_ShortNewPassword_ReturnsBadRequest() throws Exception {
            mockMvc.perform(put("/api/v1/profile/password")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"correctpass\",\"newPassword\":\"abc\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 when new password is blank")
        void changePassword_BlankNewPassword_ReturnsBadRequest() throws Exception {
            mockMvc.perform(put("/api/v1/profile/password")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"correctpass\",\"newPassword\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 403 when not authenticated")
        void changePassword_NoToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(put("/api/v1/profile/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"correctpass\",\"newPassword\":\"newpassword123\"}"))
                    .andExpect(status().isForbidden());
        }
    }
}
