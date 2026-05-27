package com.example.demo.task.repository;

import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.security.repository.UserRepository;
import com.example.demo.task.entity.Task;
import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TaskSpecification — the JPA query filter building blocks.
 *
 * WHY @DataJpaTest:
 *   We use @DataJpaTest instead of full @SpringBootTest because we only need
 *   the database layer (no web layer, no security, no services). This makes
 *   tests run much faster and keeps them focused on query logic.
 *
 * WHY this matters:
 *   Every filter (status, priority, category, archived, date range, search) is
 *   tested against a real (H2) database to ensure the predicates produce correct
 *   SQL. Without these tests, a typo in a field name or predicate logic would
 *   silently return wrong data to users.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TaskSpecification Integration Tests")
class TaskSpecificationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User();
        userA.setUsername("userA");
        userA.setPassword("password");
        userA.setRole(Role.ROLE_USER);
        userRepository.save(userA);

        userB = new User();
        userB.setUsername("userB");
        userB.setPassword("password");
        userB.setRole(Role.ROLE_USER);
        userRepository.save(userB);
    }

    // Helper to create and save a task
    private Task createTask(User owner, String title, TaskStatus status,
                            TaskPriority priority, String category,
                            boolean archived, LocalDate dueDate) {
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(status);
        task.setPriority(priority);
        task.setCategory(category);
        task.setArchived(archived);
        task.setDueDate(dueDate);
        task.setUser(owner);
        return taskRepository.save(task);
    }

    // ============================================================
    // belongsToUser Specification Tests
    // ============================================================

    @Nested
    @DisplayName("belongsToUser specification")
    class BelongsToUser {

        @Test
        @DisplayName("Returns only tasks belonging to the specified user")
        void returnsOnlyUserATasks() {
            createTask(userA, "UserA Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userB, "UserB Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.belongsToUser(userA.getId()));

            assertEquals(1, results.size());
            assertEquals("UserA Task", results.get(0).getTitle());
        }

        @Test
        @DisplayName("Returns all tasks when userId is null (admin bypass)")
        void returnsAllTasksWhenUserIdIsNull() {
            createTask(userA, "UserA Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userB, "UserB Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.belongsToUser(null));

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Returns empty list when user has no tasks")
        void returnsEmptyForUserWithNoTasks() {
            createTask(userB, "UserB Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.belongsToUser(userA.getId()));

            assertTrue(results.isEmpty());
        }
    }

    // ============================================================
    // hasStatus Specification Tests
    // ============================================================

    @Nested
    @DisplayName("hasStatus specification")
    class HasStatus {

        @Test
        @DisplayName("Returns only tasks with the given status")
        void returnsOnlyPendingTasks() {
            createTask(userA, "Pending Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Done Task", TaskStatus.COMPLETED, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasStatus(TaskStatus.PENDING));

            assertEquals(1, results.size());
            assertEquals(TaskStatus.PENDING, results.get(0).getStatus());
        }

        @Test
        @DisplayName("Returns all tasks when status is null (no filter)")
        void returnsAllWhenStatusIsNull() {
            createTask(userA, "Pending Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Done Task", TaskStatus.COMPLETED, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasStatus(null));

            assertEquals(2, results.size());
        }
    }

    // ============================================================
    // hasPriority Specification Tests
    // ============================================================

    @Nested
    @DisplayName("hasPriority specification")
    class HasPriority {

        @Test
        @DisplayName("Returns only HIGH priority tasks")
        void returnsOnlyHighPriorityTasks() {
            createTask(userA, "High Task", TaskStatus.PENDING, TaskPriority.HIGH, null, false, null);
            createTask(userA, "Low Task", TaskStatus.PENDING, TaskPriority.LOW, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasPriority(TaskPriority.HIGH));

            assertEquals(1, results.size());
            assertEquals(TaskPriority.HIGH, results.get(0).getPriority());
        }

        @Test
        @DisplayName("Returns all tasks when priority is null (no filter)")
        void returnsAllWhenPriorityIsNull() {
            createTask(userA, "High Task", TaskStatus.PENDING, TaskPriority.HIGH, null, false, null);
            createTask(userA, "Low Task", TaskStatus.PENDING, TaskPriority.LOW, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasPriority(null));

            assertEquals(2, results.size());
        }
    }

    // ============================================================
    // hasCategory Specification Tests
    // ============================================================

    @Nested
    @DisplayName("hasCategory specification")
    class HasCategory {

        @Test
        @DisplayName("Returns tasks matching the exact category")
        void returnsCategoryMatch() {
            createTask(userA, "Work Task", TaskStatus.PENDING, TaskPriority.MEDIUM, "Work", false, null);
            createTask(userA, "Personal Task", TaskStatus.PENDING, TaskPriority.MEDIUM, "Personal", false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasCategory("Work"));

            assertEquals(1, results.size());
            assertEquals("Work", results.get(0).getCategory());
        }

        @Test
        @DisplayName("Returns all tasks when category is null")
        void returnsAllWhenCategoryIsNull() {
            createTask(userA, "Work Task", TaskStatus.PENDING, TaskPriority.MEDIUM, "Work", false, null);
            createTask(userA, "Personal Task", TaskStatus.PENDING, TaskPriority.MEDIUM, "Personal", false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasCategory(null));

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Returns all tasks when category is empty string")
        void returnsAllWhenCategoryIsEmpty() {
            createTask(userA, "Work Task", TaskStatus.PENDING, TaskPriority.MEDIUM, "Work", false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasCategory(""));

            assertEquals(1, results.size()); // no filter applied
        }
    }

    // ============================================================
    // isArchived Specification Tests
    // ============================================================

    @Nested
    @DisplayName("isArchived specification")
    class IsArchived {

        @Test
        @DisplayName("Returns only non-archived tasks when archived=false")
        void returnsOnlyActiveTasksWhenArchivedFalse() {
            createTask(userA, "Active Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Archived Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, true, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.isArchived(false));

            assertEquals(1, results.size());
            assertFalse(results.get(0).isArchived());
        }

        @Test
        @DisplayName("Returns only archived tasks when archived=true")
        void returnsOnlyArchivedTasksWhenArchivedTrue() {
            createTask(userA, "Active Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Archived Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, true, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.isArchived(true));

            assertEquals(1, results.size());
            assertTrue(results.get(0).isArchived());
        }

        @Test
        @DisplayName("Defaults to non-archived tasks when archived=null")
        void defaultsToNonArchivedWhenNull() {
            createTask(userA, "Active Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Archived Task", TaskStatus.PENDING, TaskPriority.MEDIUM, null, true, null);

            // null means "show active" — the default behavior
            List<Task> results = taskRepository.findAll(TaskSpecification.isArchived(null));

            assertEquals(1, results.size());
            assertFalse(results.get(0).isArchived());
        }
    }

    // ============================================================
    // searchKeyword Specification Tests
    // ============================================================

    @Nested
    @DisplayName("searchKeyword specification")
    class SearchKeyword {

        @Test
        @DisplayName("Finds tasks whose title contains the keyword (case-insensitive)")
        void findsByTitleKeyword() {
            createTask(userA, "Fix Login Bug", TaskStatus.PENDING, TaskPriority.HIGH, null, false, null);
            createTask(userA, "Update Dashboard", TaskStatus.PENDING, TaskPriority.LOW, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.searchKeyword("login"));

            assertEquals(1, results.size());
            assertTrue(results.get(0).getTitle().contains("Login"));
        }

        @Test
        @DisplayName("Returns all tasks when keyword is null")
        void returnsAllWhenKeywordIsNull() {
            createTask(userA, "Task One", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Task Two", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.searchKeyword(null));

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Returns all tasks when keyword is empty string")
        void returnsAllWhenKeywordIsEmpty() {
            createTask(userA, "Task One", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.searchKeyword(""));

            assertEquals(1, results.size());
        }
    }

    // ============================================================
    // hasDueDateRange Specification Tests
    // ============================================================

    @Nested
    @DisplayName("hasDueDateRange specification")
    class HasDueDateRange {

        @Test
        @DisplayName("Returns tasks within a date range (inclusive)")
        void returnsTasksWithinRange() {
            LocalDate today = LocalDate.now();
            createTask(userA, "Due Yesterday", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, today.minusDays(1));
            createTask(userA, "Due Today", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, today);
            createTask(userA, "Due Tomorrow", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, today.plusDays(1));
            createTask(userA, "Due Next Week", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, today.plusDays(7));

            Specification<Task> spec = TaskSpecification.hasDueDateRange(today, today.plusDays(1));
            List<Task> results = taskRepository.findAll(spec);

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Returns all tasks when both dates are null")
        void returnsAllWhenBothDatesNull() {
            createTask(userA, "Task 1", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, LocalDate.now());
            createTask(userA, "Task 2", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);

            List<Task> results = taskRepository.findAll(TaskSpecification.hasDueDateRange(null, null));

            assertEquals(2, results.size());
        }
    }

    // ============================================================
    // Combined Specifications Tests
    // ============================================================

    @Nested
    @DisplayName("Combined specifications")
    class CombinedSpecifications {

        @Test
        @DisplayName("Combining user + status + archived filters correctly")
        void combinesUserStatusAndArchivedFilters() {
            // UserA has: 1 pending active, 1 completed active, 1 archived pending
            createTask(userA, "Pending Active", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Completed Active", TaskStatus.COMPLETED, TaskPriority.MEDIUM, null, false, null);
            createTask(userA, "Pending Archived", TaskStatus.PENDING, TaskPriority.MEDIUM, null, true, null);
            // UserB has: 1 pending active
            createTask(userB, "UserB Pending", TaskStatus.PENDING, TaskPriority.MEDIUM, null, false, null);

            Specification<Task> spec = Specification.allOf(
                    TaskSpecification.belongsToUser(userA.getId()),
                    TaskSpecification.hasStatus(TaskStatus.PENDING),
                    TaskSpecification.isArchived(false)
            );

            List<Task> results = taskRepository.findAll(spec);

            assertEquals(1, results.size());
            assertEquals("Pending Active", results.get(0).getTitle());
        }
    }
}
