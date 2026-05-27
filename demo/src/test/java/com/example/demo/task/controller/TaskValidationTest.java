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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Edge case and validation tests for task creation.
 *
 * WHY this matters:
 *   Jakarta Validation annotations on TaskRequestDTO are the primary defense against
 *   malformed or malicious input. These tests ensure every constraint is actually enforced.
 *   Without these, a silent framework misconfiguration could let invalid data into the database.
 *
 *   Each test also verifies the field-level error response structure that the frontend depends on.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Task Validation & Edge Case Tests")
class TaskValidationTest {

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

    private String userToken;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("validationuser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);
        userToken = "Bearer " + jwtService.generateToken(user);
    }

    // ============================================================
    // Title Validation
    // ============================================================

    @Nested
    @DisplayName("Title field validation")
    class TitleValidation {

        @Test
        @DisplayName("Returns 400 when title is empty string")
        void emptyTitle_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.title").exists());
        }

        @Test
        @DisplayName("Returns 400 when title is only whitespace")
        void whitespaceTitle_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"   \",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.title").exists());
        }

        @Test
        @DisplayName("Returns 400 when title exceeds 255 characters")
        void tooLongTitle_ReturnsBadRequest() throws Exception {
            String longTitle = "A".repeat(256);
            String body = String.format("{\"title\":\"%s\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}", longTitle);

            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.title").exists());
        }

        @Test
        @DisplayName("Accepts title at exactly 255 characters (boundary)")
        void maxLengthTitle_ReturnsCreated() throws Exception {
            String maxTitle = "A".repeat(255);
            String body = String.format("{\"title\":\"%s\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}", maxTitle);

            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }
    }

    // ============================================================
    // Status Validation
    // ============================================================

    @Nested
    @DisplayName("Status field validation")
    class StatusValidation {

        @Test
        @DisplayName("Returns 400 when status is an invalid enum value")
        void invalidStatus_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Task\",\"status\":\"INVALID_STATUS\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.status").exists());
        }

        @Test
        @DisplayName("Returns 400 when status is missing entirely")
        void missingStatus_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Task\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Accepts lowercase status 'pending' (validator is case-insensitive)")
        void lowercaseStatus_ReturnsCreated() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Task\",\"status\":\"pending\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isCreated());
        }
    }

    // ============================================================
    // Priority Validation
    // ============================================================

    @Nested
    @DisplayName("Priority field validation")
    class PriorityValidation {

        @Test
        @DisplayName("Returns 400 when priority is an invalid enum value")
        void invalidPriority_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Task\",\"status\":\"PENDING\",\"priority\":\"URGENT\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.priority").exists());
        }

        @Test
        @DisplayName("Returns 400 when priority is missing entirely")
        void missingPriority_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Task\",\"status\":\"PENDING\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // Description / Category Validation
    // ============================================================

    @Nested
    @DisplayName("Description and Category field validation")
    class DescriptionCategoryValidation {

        @Test
        @DisplayName("Returns 400 when description exceeds 1000 characters")
        void tooLongDescription_ReturnsBadRequest() throws Exception {
            String longDesc = "D".repeat(1001);
            String body = String.format(
                    "{\"title\":\"Task\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\",\"description\":\"%s\"}",
                    longDesc
            );

            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.description").exists());
        }

        @Test
        @DisplayName("Returns 400 when category exceeds 100 characters")
        void tooLongCategory_ReturnsBadRequest() throws Exception {
            String longCategory = "C".repeat(101);
            String body = String.format(
                    "{\"title\":\"Task\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\",\"category\":\"%s\"}",
                    longCategory
            );

            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.category").exists());
        }

        @Test
        @DisplayName("Accepts task with optional description and category omitted")
        void optionalFieldsOmitted_ReturnsCreated() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Minimal Task\",\"status\":\"PENDING\",\"priority\":\"LOW\"}"))
                    .andExpect(status().isCreated());
        }
    }

    // ============================================================
    // Malformed / Empty Payload Tests
    // ============================================================

    @Nested
    @DisplayName("Malformed payload edge cases")
    class MalformedPayload {

        @Test
        @DisplayName("Returns 400 for completely empty JSON object")
        void emptyJsonObject_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 for missing Content-Type header (non-JSON request)")
        void missingContentType_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .content("{\"title\":\"Task\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Returns 400 for multiple missing required fields at once")
        void multipleFieldsMissing_ReturnsBadRequestWithDetails() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details").isMap());
        }
    }
}
