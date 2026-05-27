package com.example.demo.task.service;

import com.example.demo.exception.TaskNotFoundException;
import com.example.demo.security.entity.Role;
import com.example.demo.security.entity.User;
import com.example.demo.task.dto.TaskRequestDTO;
import com.example.demo.task.dto.TaskResponseDTO;
import com.example.demo.task.dto.UserProfileResponseDTO;
import com.example.demo.task.entity.Task;
import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.entity.TaskStatus;
import com.example.demo.task.mapper.TaskMapper;
import com.example.demo.task.repository.TaskActivityRepository;
import com.example.demo.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService business logic.
 * We test each method in isolation using mocks — no database or Spring context needed.
 * This makes tests fast, deterministic, and beginner-friendly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService Unit Tests")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskActivityRepository taskActivityRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskResponseDTO responseDTO;
    private TaskRequestDTO requestDTO;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(10L);
        user.setUsername("testuser");
        user.setRole(Role.ROLE_USER);

        task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(TaskPriority.MEDIUM);
        task.setArchived(false);
        task.setUser(user);

        responseDTO = new TaskResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setTitle("Test Task");
        responseDTO.setStatus(TaskStatus.PENDING);

        requestDTO = new TaskRequestDTO();
        requestDTO.setTitle("Test Task");
        requestDTO.setStatus("PENDING");
        requestDTO.setPriority("MEDIUM");
    }

    // ============================================================
    // getTaskById Tests
    // ============================================================

    @Nested
    @DisplayName("getTaskById")
    class GetTaskById {

        @Test
        @DisplayName("Returns TaskResponseDTO when task exists")
        void getTaskById_WhenTaskExists_ReturnsTaskResponse() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(taskMapper.toResponseDTO(task)).thenReturn(responseDTO);

            TaskResponseDTO result = taskService.getTaskById(1L);

            assertNotNull(result);
            assertEquals("Test Task", result.getTitle());
            verify(taskRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Throws TaskNotFoundException when task does not exist")
        void getTaskById_WhenTaskDoesNotExist_ThrowsTaskNotFoundException() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            TaskNotFoundException exception = assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.getTaskById(99L)
            );

            assertTrue(exception.getMessage().contains("99"),
                    "Exception message should contain the missing task ID");
        }
    }

    // ============================================================
    // createTask Tests
    // ============================================================

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("Creates task, assigns user, logs activity, and returns DTO")
        void createTask_ValidInput_CreatesTaskWithUser() {
            Task newTask = new Task();
            newTask.setId(2L);
            newTask.setTitle("My Task");
            newTask.setUser(user);

            TaskResponseDTO newResponseDTO = new TaskResponseDTO();
            newResponseDTO.setId(2L);
            newResponseDTO.setTitle("My Task");

            when(taskMapper.toEntity(requestDTO)).thenReturn(newTask);
            when(taskRepository.save(newTask)).thenReturn(newTask);
            when(taskMapper.toResponseDTO(newTask)).thenReturn(newResponseDTO);

            TaskResponseDTO result = taskService.createTask(requestDTO, user);

            assertNotNull(result);
            assertEquals(2L, result.getId());
            // Verify the task was assigned to the correct user
            assertEquals(user, newTask.getUser());
            // Verify the task was saved
            verify(taskRepository, times(1)).save(newTask);
            // Verify an activity log was saved
            verify(taskActivityRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Newly created task is never archived")
        void createTask_NewTask_IsNotArchived() {
            Task newTask = new Task();
            newTask.setId(3L);

            when(taskMapper.toEntity(requestDTO)).thenReturn(newTask);
            when(taskRepository.save(newTask)).thenReturn(newTask);
            when(taskMapper.toResponseDTO(newTask)).thenReturn(responseDTO);

            taskService.createTask(requestDTO, user);

            // createTask must set archived=false regardless of input
            assertFalse(newTask.isArchived(),
                    "A newly created task must always start as non-archived");
        }
    }

    // ============================================================
    // updateTask Tests
    // ============================================================

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("Throws TaskNotFoundException when task does not exist")
        void updateTask_WhenTaskDoesNotExist_ThrowsTaskNotFoundException() {
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.updateTask(1L, requestDTO, user)
            );
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when trying to update an archived task")
        void updateTask_WhenTaskIsArchived_ThrowsIllegalArgumentException() {
            task.setArchived(true);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> taskService.updateTask(1L, requestDTO, user)
            );

            assertTrue(exception.getMessage().toLowerCase().contains("archived"),
                    "Exception message should mention 'archived'");
        }

        @Test
        @DisplayName("Saves and logs activity when update is successful")
        void updateTask_ValidTask_SavesAndLogsActivity() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskMapper.toResponseDTO(task)).thenReturn(responseDTO);

            taskService.updateTask(1L, requestDTO, user);

            verify(taskRepository, times(1)).save(task);
            verify(taskActivityRepository, times(1)).save(any());
        }
    }

    // ============================================================
    // archiveTask Tests
    // ============================================================

    @Nested
    @DisplayName("archiveTask")
    class ArchiveTask {

        @Test
        @DisplayName("Sets archived=true and logs activity")
        void archiveTask_ActiveTask_SetsArchivedTrue() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskMapper.toResponseDTO(task)).thenReturn(responseDTO);

            taskService.archiveTask(1L, user);

            assertTrue(task.isArchived(), "Task should be marked as archived");
            verify(taskActivityRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Throws TaskNotFoundException when task does not exist")
        void archiveTask_NotExistingTask_ThrowsNotFoundException() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.archiveTask(99L, user)
            );
        }
    }

    // ============================================================
    // restoreTask Tests
    // ============================================================

    @Nested
    @DisplayName("restoreTask")
    class RestoreTask {

        @Test
        @DisplayName("Sets archived=false and logs activity")
        void restoreTask_ArchivedTask_SetsArchivedFalse() {
            task.setArchived(true);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskMapper.toResponseDTO(task)).thenReturn(responseDTO);

            taskService.restoreTask(1L, user);

            assertFalse(task.isArchived(), "Task should be restored (archived=false)");
            verify(taskActivityRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Throws TaskNotFoundException when task does not exist")
        void restoreTask_NotExistingTask_ThrowsNotFoundException() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.restoreTask(99L, user)
            );
        }
    }

    // ============================================================
    // deleteTask Tests
    // ============================================================

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("Deletes task and logs activity")
        void deleteTask_ExistingTask_DeletesAndLogs() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            boolean result = taskService.deleteTask(1L, user);

            assertTrue(result, "deleteTask should return true on success");
            verify(taskRepository, times(1)).delete(task);
            verify(taskActivityRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Throws TaskNotFoundException when task does not exist")
        void deleteTask_NotExistingTask_ThrowsNotFoundException() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.deleteTask(99L, user)
            );
        }
    }

    // ============================================================
    // getUserStats Tests
    // ============================================================

    @Nested
    @DisplayName("getUserStats")
    class GetUserStats {

        @Test
        @DisplayName("Returns stats DTO with correct username and role")
        @SuppressWarnings("unchecked")
        void getUserStats_ReturnsCorrectUserDetails() {
            // Stub count queries — any Specification input returns 0
            when(taskRepository.count(any(Specification.class))).thenReturn(0L);

            UserProfileResponseDTO stats = taskService.getUserStats(user);

            assertNotNull(stats);
            assertEquals("testuser", stats.getUsername());
            assertEquals("ROLE_USER", stats.getRole());
            assertEquals(user.getId(), stats.getId());
        }

        @Test
        @DisplayName("Returns zero counts when user has no tasks")
        @SuppressWarnings("unchecked")
        void getUserStats_NoTasks_ReturnsZeroCounts() {
            when(taskRepository.count(any(Specification.class))).thenReturn(0L);

            UserProfileResponseDTO stats = taskService.getUserStats(user);

            assertEquals(0L, stats.getTotalTasks());
            assertEquals(0L, stats.getCompletedTasks());
            assertEquals(0L, stats.getOverdueTasks());
        }
    }
}
