package com.example.demo.task.controller;

import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import com.example.demo.security.service.JwtService;
import com.example.demo.task.entity.Task;
import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.entity.TaskStatus;
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
 * Integration tests for TaskController — tests the full HTTP request/response cycle.
 *
 * Uses @SpringBootTest + MockMvc to load the real application context with an
 * in-memory H2 database. Each test creates its own clean database state via @BeforeEach.
 *
 * WHY this matters:
 *   Controller tests catch bugs that unit tests miss: wrong URL mappings, missing
 *   security annotations, incorrect HTTP status codes, and broken serialization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TaskController Integration Tests")
class TaskControllerTest {

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
    private String adminToken;
    private User testUser;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(Role.ROLE_USER);
        userRepository.save(testUser);
        userToken = "Bearer " + jwtService.generateToken(testUser);

        testAdmin = new User();
        testAdmin.setUsername("testadmin");
        testAdmin.setPassword(passwordEncoder.encode("password123"));
        testAdmin.setRole(Role.ROLE_ADMIN);
        userRepository.save(testAdmin);
        adminToken = "Bearer " + jwtService.generateToken(testAdmin);
    }

    /** Helper: saves a task owned by the given user */
    private Task createTaskFor(User owner, String title, TaskStatus status, boolean archived) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(status);
        task.setPriority(TaskPriority.MEDIUM);
        task.setArchived(archived);
        task.setUser(owner);
        return taskRepository.save(task);
    }

    // ============================================================
    // GET /api/v1/tasks
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/tasks")
    class GetTasks {

        @Test
        @DisplayName("Returns 403 when no token is provided")
        void getTasks_WithoutToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/tasks"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 200 and empty page for authenticated user with no tasks")
        void getTasks_WithValidToken_ReturnsOk() throws Exception {
            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("Filters by status parameter correctly")
        void getTasks_WithStatusFilter_ReturnsFilteredResults() throws Exception {
            createTaskFor(testUser, "Pending Task", TaskStatus.PENDING, false);
            createTaskFor(testUser, "Done Task", TaskStatus.COMPLETED, false);

            mockMvc.perform(get("/api/v1/tasks?status=PENDING")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("User cannot see another user's tasks")
        void getTasks_UserCannotSeeOtherUserTasks() throws Exception {
            User otherUser = new User();
            otherUser.setUsername("otheruser");
            otherUser.setPassword(passwordEncoder.encode("pass"));
            otherUser.setRole(Role.ROLE_USER);
            userRepository.save(otherUser);

            createTaskFor(otherUser, "Other User Task", TaskStatus.PENDING, false);

            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("Admin can see all tasks across all users")
        void getTasks_AdminCanSeeAllTasks() throws Exception {
            createTaskFor(testUser, "User Task", TaskStatus.PENDING, false);

            mockMvc.perform(get("/api/v1/tasks")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0]").exists());
        }
    }

    // ============================================================
    // POST /api/v1/tasks
    // ============================================================

    @Nested
    @DisplayName("POST /api/v1/tasks")
    class CreateTask {

        @Test
        @DisplayName("Returns 201 Created when payload is valid")
        void createTask_WithValidPayload_ReturnsCreated() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"New Task\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("New Task"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("Returns 400 when title is blank")
        void createTask_WithBlankTitle_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 403 when not authenticated")
        void createTask_WithoutToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Task\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // GET /api/v1/tasks/{id}
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/tasks/{id}")
    class GetTaskById {

        @Test
        @DisplayName("Returns 403 when user tries to access another user's task")
        void getTask_ByNonOwner_ReturnsForbidden() throws Exception {
            User otherUser = new User();
            otherUser.setUsername("otheruser2");
            otherUser.setPassword(passwordEncoder.encode("pass"));
            otherUser.setRole(Role.ROLE_USER);
            userRepository.save(otherUser);

            Task otherTask = createTaskFor(otherUser, "Other Task", TaskStatus.PENDING, false);

            mockMvc.perform(get("/api/v1/tasks/" + otherTask.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 200 when owner accesses their own task")
        void getTask_ByOwner_ReturnsOk() throws Exception {
            Task myTask = createTaskFor(testUser, "My Task", TaskStatus.PENDING, false);

            mockMvc.perform(get("/api/v1/tasks/" + myTask.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("My Task"));
        }

        @Test
        @DisplayName("Returns 403 when accessing a non-existent task ID (security check fires first)")
        void getTask_NonExistentId_ReturnsForbidden() throws Exception {
            // When the task doesn't exist, @taskSecurity.isOwnerOrAdmin cannot verify ownership
            // and denies access with 403 before the service layer can throw 404.
            // This is the correct security-first behavior.
            mockMvc.perform(get("/api/v1/tasks/999999")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // PUT /api/v1/tasks/{id}
    // ============================================================

    @Nested
    @DisplayName("PUT /api/v1/tasks/{id}")
    class UpdateTask {

        @Test
        @DisplayName("Returns 200 when owner updates their own task")
        void updateTask_ByOwner_ReturnsOk() throws Exception {
            Task task = createTaskFor(testUser, "Old Title", TaskStatus.PENDING, false);

            mockMvc.perform(put("/api/v1/tasks/" + task.getId())
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Updated Title\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Updated Title"))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Returns 400 when trying to update an archived task")
        void updateTask_ArchivedTask_ReturnsBadRequest() throws Exception {
            Task archivedTask = createTaskFor(testUser, "Archived Task", TaskStatus.PENDING, true);

            mockMvc.perform(put("/api/v1/tasks/" + archivedTask.getId())
                            .header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Try Update\",\"status\":\"PENDING\",\"priority\":\"MEDIUM\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============================================================
    // PUT /api/v1/tasks/{id}/archive and /restore
    // ============================================================

    @Nested
    @DisplayName("Archive and Restore endpoints")
    class ArchiveRestore {

        @Test
        @DisplayName("Returns 200 when archiving an owned task")
        void archiveTask_ByOwner_ReturnsOk() throws Exception {
            Task task = createTaskFor(testUser, "Active Task", TaskStatus.PENDING, false);

            mockMvc.perform(put("/api/v1/tasks/" + task.getId() + "/archive")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.archived").value(true));
        }

        @Test
        @DisplayName("Returns 200 when restoring an archived task")
        void restoreTask_ByOwner_ReturnsOk() throws Exception {
            Task task = createTaskFor(testUser, "Archived Task", TaskStatus.PENDING, true);

            mockMvc.perform(put("/api/v1/tasks/" + task.getId() + "/restore")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.archived").value(false));
        }

        @Test
        @DisplayName("Returns 403 when non-owner tries to archive a task")
        void archiveTask_ByNonOwner_ReturnsForbidden() throws Exception {
            User otherUser = new User();
            otherUser.setUsername("archiveuser");
            otherUser.setPassword(passwordEncoder.encode("pass"));
            otherUser.setRole(Role.ROLE_USER);
            userRepository.save(otherUser);

            Task task = createTaskFor(otherUser, "Other's Task", TaskStatus.PENDING, false);

            mockMvc.perform(put("/api/v1/tasks/" + task.getId() + "/archive")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // DELETE /api/v1/tasks/{id}
    // ============================================================

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("Returns 200 when owner deletes their task")
        void deleteTask_ByOwner_ReturnsOk() throws Exception {
            Task task = createTaskFor(testUser, "To Be Deleted", TaskStatus.PENDING, false);

            mockMvc.perform(delete("/api/v1/tasks/" + task.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Returns 403 when non-owner tries to delete a task")
        void deleteTask_ByNonOwner_ReturnsForbidden() throws Exception {
            User otherUser = new User();
            otherUser.setUsername("deleteuser");
            otherUser.setPassword(passwordEncoder.encode("pass"));
            otherUser.setRole(Role.ROLE_USER);
            userRepository.save(otherUser);

            Task task = createTaskFor(otherUser, "Other Task", TaskStatus.PENDING, false);

            mockMvc.perform(delete("/api/v1/tasks/" + task.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // GET /api/v1/tasks/stats
    // ============================================================

    @Nested
    @DisplayName("GET /api/v1/tasks/stats")
    class GetStats {

        @Test
        @DisplayName("Returns 200 with user stats for authenticated user")
        void getStats_AuthenticatedUser_ReturnsOk() throws Exception {
            createTaskFor(testUser, "Pending Task", TaskStatus.PENDING, false);
            createTaskFor(testUser, "Done Task", TaskStatus.COMPLETED, false);

            mockMvc.perform(get("/api/v1/tasks/stats")
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.totalTasks").value(2));
        }

        @Test
        @DisplayName("Returns 403 when no token is provided")
        void getStats_WithoutToken_ReturnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/tasks/stats"))
                    .andExpect(status().isForbidden());
        }
    }
}
