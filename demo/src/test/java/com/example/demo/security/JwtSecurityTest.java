package com.example.demo.security;

import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import com.example.demo.security.service.JwtService;
import com.example.demo.task.entity.Task;
import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.entity.TaskStatus;
import com.example.demo.task.repository.TaskRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security-focused tests — validates JWT handling and access control enforcement.
 *
 * WHY this matters:
 *   These tests act as a permanent regression guard against security regressions.
 *   If someone accidentally removes a @PreAuthorize, changes a security config,
 *   or breaks JWT validation, these tests will catch it immediately.
 *
 *   Scenarios covered:
 *   - Missing token → 403
 *   - Malformed token → 403
 *   - Expired token → 403
 *   - Valid token with wrong user → 403 (ownership enforcement)
 *   - Valid token correct user → 200
 *   - Admin token → can access any task
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("JWT Security Integration Tests")
public class JwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${security.jwt.secret:Y2hvb25hLWxvb25hLW1vb25hLXNvb25hLXRvb25hLXdvb25hLWNhcmQ=}")
    private String secretKey;

    private User testUser;
    private User adminUser;
    private String validUserToken;
    private String validAdminToken;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("securityuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(Role.ROLE_USER);
        userRepository.save(testUser);
        validUserToken = "Bearer " + jwtService.generateToken(testUser);

        adminUser = new User();
        adminUser.setUsername("securityadmin");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setRole(Role.ROLE_ADMIN);
        userRepository.save(adminUser);
        validAdminToken = "Bearer " + jwtService.generateToken(adminUser);
    }

    /** Helper: creates a task owned by the given user */
    private Task createTaskFor(User owner) {
        Task task = new Task();
        task.setTitle("Security Test Task");
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(TaskPriority.MEDIUM);
        task.setArchived(false);
        task.setUser(owner);
        return taskRepository.save(task);
    }

    /** Creates a JWT token that is already expired */
    private String createExpiredToken(User user) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return "Bearer " + Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // expired 1 hour ago
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // ============================================================
    // Missing / Malformed / Expired Token Tests
    // ============================================================

    @Nested
    @DisplayName("Token rejection scenarios")
    class TokenRejection {

        @Test
        @DisplayName("Returns 403 when no Authorization header is present")
        void noToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/tasks"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token is completely random gibberish")
        void malformedToken_Gibberish_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token has 'Bearer ' prefix but empty value")
        void malformedToken_EmptyBearer_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", "Bearer "))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token is sent without 'Bearer ' prefix")
        void malformedToken_NoPrefix_ReturnsForbidden() throws Exception {
            String rawToken = jwtService.generateToken(testUser);
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", rawToken)) // no "Bearer " prefix
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when token is expired")
        void expiredToken_ReturnsForbidden() throws Exception {
            String expiredToken = createExpiredToken(testUser);

            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", expiredToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when Authorization header is just 'null'")
        void nullStringToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", "null"))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // Valid Token - Access Control Tests
    // ============================================================

    @Nested
    @DisplayName("Valid token — access control")
    class ValidTokenAccessControl {

        @Test
        @DisplayName("Returns 200 when authenticated user accesses task list")
        void validToken_GetTasks_ReturnsOk() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", validUserToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Returns 200 when user accesses their own task")
        void validToken_OwnTask_ReturnsOk() throws Exception {
            Task myTask = createTaskFor(testUser);

            mockMvc.perform(get("/api/v1/tasks/" + myTask.getId())
                            .header("Authorization", validUserToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Returns 403 when user tries to access another user's task (privilege escalation)")
        void validToken_OtherUserTask_ReturnsForbidden() throws Exception {
            Task adminTask = createTaskFor(adminUser);

            mockMvc.perform(get("/api/v1/tasks/" + adminTask.getId())
                            .header("Authorization", validUserToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin token can access any task regardless of owner")
        void adminToken_AnyTask_ReturnsOk() throws Exception {
            Task userTask = createTaskFor(testUser);

            mockMvc.perform(get("/api/v1/tasks/" + userTask.getId())
                            .header("Authorization", validAdminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Returns 403 when user tries to delete another user's task")
        void validToken_DeleteOtherUserTask_ReturnsForbidden() throws Exception {
            Task adminTask = createTaskFor(adminUser);

            mockMvc.perform(delete("/api/v1/tasks/" + adminTask.getId())
                            .header("Authorization", validUserToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when user tries to update another user's task")
        void validToken_UpdateOtherUserTask_ReturnsForbidden() throws Exception {
            Task adminTask = createTaskFor(adminUser);

            mockMvc.perform(put("/api/v1/tasks/" + adminTask.getId())
                            .header("Authorization", validUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Hacked\",\"status\":\"PENDING\",\"priority\":\"HIGH\"}"))
                    .andExpect(status().isForbidden());
        }
    }
}
