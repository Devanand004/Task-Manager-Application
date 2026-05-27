package com.example.demo.task.service;

import com.example.demo.exception.TaskNotFoundException;
import com.example.demo.task.dto.TaskRequestDTO;
import com.example.demo.task.dto.TaskResponseDTO;
import com.example.demo.task.dto.UserProfileResponseDTO;
import com.example.demo.task.entity.Task;
import com.example.demo.task.entity.TaskStatus;
import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.entity.TaskActivity;
import com.example.demo.task.mapper.TaskMapper;
import com.example.demo.task.repository.TaskRepository;
import com.example.demo.task.repository.TaskSpecification;
import com.example.demo.task.repository.TaskActivityRepository;
import com.example.demo.security.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@CacheConfig(cacheNames = "tasks")
@Transactional(readOnly = true)
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final TaskActivityRepository taskActivityRepository;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper, TaskActivityRepository taskActivityRepository) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.taskActivityRepository = taskActivityRepository;
    }

    public Page<TaskResponseDTO> getTasks(Long userId, TaskStatus status, TaskPriority priority, String category, Boolean archived, LocalDate dueDate, LocalDate startDate, LocalDate endDate, String search, Pageable pageable) {
        Specification<Task> spec = Specification.allOf(
                TaskSpecification.belongsToUser(userId),
                TaskSpecification.hasStatus(status),
                TaskSpecification.hasPriority(priority),
                TaskSpecification.hasCategory(category),
                TaskSpecification.isArchived(archived),
                TaskSpecification.hasDueDate(dueDate),
                TaskSpecification.hasDueDateRange(startDate, endDate),
                TaskSpecification.searchKeyword(search)
        );
        
        return taskRepository.findAll(spec, pageable)
                .map(taskMapper::toResponseDTO);
    }

    @Cacheable(key = "#id")
    public TaskResponseDTO getTaskById(Long id) {
        return taskRepository.findById(id)
                .map(taskMapper::toResponseDTO)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + id));
    }

    @Transactional
    @CachePut(key = "#result.id")
    public TaskResponseDTO createTask(TaskRequestDTO requestDTO, User user) {
        Task task = taskMapper.toEntity(requestDTO);
        task.setUser(user);
        task.setArchived(false);
        Task savedTask = taskRepository.save(task);

        MDC.put("taskId", String.valueOf(savedTask.getId()));
        MDC.put("userId", String.valueOf(user.getId()));
        MDC.put("action", "CREATE");
        try {
            log.info("Task created: title='{}' by user='{}'", savedTask.getTitle(), user.getUsername());
            logActivity(savedTask.getId(), user, "CREATED", "Task created: " + savedTask.getTitle());
        } finally {
            MDC.remove("taskId");
            MDC.remove("userId");
            MDC.remove("action");
        }

        return taskMapper.toResponseDTO(savedTask);
    }

    @Transactional
    @CachePut(key = "#id")
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDTO, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + id));

        if (task.isArchived()) {
            throw new IllegalArgumentException("Cannot update an archived task. Please restore it first.");
        }

        TaskStatus oldStatus = task.getStatus();
        TaskPriority oldPriority = task.getPriority();

        taskMapper.updateEntityFromDto(requestDTO, task);
        Task updatedTask = taskRepository.save(task);

        MDC.put("taskId", String.valueOf(id));
        MDC.put("userId", String.valueOf(user.getId()));
        MDC.put("action", "UPDATE");
        try {
            StringBuilder details = new StringBuilder("Task updated.");
            if (oldStatus != updatedTask.getStatus()) {
                details.append(" Status changed from ").append(oldStatus).append(" to ").append(updatedTask.getStatus()).append(".");
                log.info("Task status changed: taskId={} from={} to={} by user='{}'",
                        id, oldStatus, updatedTask.getStatus(), user.getUsername());
                logActivity(id, user, "STATUS_CHANGED", details.toString());
            } else if (oldPriority != updatedTask.getPriority()) {
                details.append(" Priority changed from ").append(oldPriority).append(" to ").append(updatedTask.getPriority()).append(".");
                log.info("Task priority changed: taskId={} from={} to={} by user='{}'",
                        id, oldPriority, updatedTask.getPriority(), user.getUsername());
                logActivity(id, user, "UPDATED", details.toString());
            } else {
                log.info("Task updated: taskId={} by user='{}'", id, user.getUsername());
                logActivity(id, user, "UPDATED", details.toString());
            }
        } finally {
            MDC.remove("taskId");
            MDC.remove("userId");
            MDC.remove("action");
        }

        return taskMapper.toResponseDTO(updatedTask);
    }

    @Transactional
    @CachePut(key = "#id")
    public TaskResponseDTO archiveTask(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + id));
        task.setArchived(true);
        Task saved = taskRepository.save(task);
        logActivity(id, user, "ARCHIVED", "Task archived: " + task.getTitle());
        return taskMapper.toResponseDTO(saved);
    }

    @Transactional
    @CachePut(key = "#id")
    public TaskResponseDTO restoreTask(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + id));
        task.setArchived(false);
        Task saved = taskRepository.save(task);
        logActivity(id, user, "RESTORED", "Task restored: " + task.getTitle());
        return taskMapper.toResponseDTO(saved);
    }

    @Transactional
    @CacheEvict(key = "#id")
    public boolean deleteTask(Long id, User user) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + id));
        // Log BEFORE delete so the activity record captures the task title correctly
        String taskTitle = task.getTitle();
        taskRepository.delete(task);
        MDC.put("taskId", String.valueOf(id));
        MDC.put("userId", String.valueOf(user.getId()));
        MDC.put("action", "DELETE");
        try {
            log.info("Task deleted: taskId={} title='{}' by user='{}'", id, taskTitle, user.getUsername());
            logActivity(id, user, "DELETED", "Task deleted: " + taskTitle);
        } finally {
            MDC.remove("taskId");
            MDC.remove("userId");
            MDC.remove("action");
        }
        return true;
    }

    public UserProfileResponseDTO getUserStats(User user) {
        long total = taskRepository.count(Specification.allOf(TaskSpecification.belongsToUser(user.getId()), TaskSpecification.isArchived(false)));
        long completed = taskRepository.count(Specification.allOf(TaskSpecification.belongsToUser(user.getId()), TaskSpecification.isArchived(false), TaskSpecification.hasStatus(TaskStatus.COMPLETED)));
        long pending = taskRepository.count(Specification.allOf(TaskSpecification.belongsToUser(user.getId()), TaskSpecification.isArchived(false), TaskSpecification.hasStatus(TaskStatus.PENDING)));
        long inProgress = taskRepository.count(Specification.allOf(TaskSpecification.belongsToUser(user.getId()), TaskSpecification.isArchived(false), TaskSpecification.hasStatus(TaskStatus.IN_PROGRESS)));
        
        LocalDate today = LocalDate.now();
        Specification<Task> overdueSpec = (root, query, cb) -> cb.and(
                cb.equal(root.get("user").get("id"), user.getId()),
                cb.equal(root.get("archived"), false),
                cb.notEqual(root.get("status"), TaskStatus.COMPLETED),
                cb.lessThan(root.get("dueDate"), today)
        );
        long overdue = taskRepository.count(overdueSpec);

        UserProfileResponseDTO stats = new UserProfileResponseDTO();
        stats.setId(user.getId());
        stats.setUsername(user.getUsername());
        stats.setRole(user.getRole().name());
        stats.setTotalTasks(total);
        stats.setCompletedTasks(completed);
        stats.setPendingTasks(pending + inProgress);
        stats.setOverdueTasks(overdue);
        return stats;
    }

    private void logActivity(Long taskId, User user, String action, String details) {
        TaskActivity activity = new TaskActivity();
        activity.setTaskId(taskId);
        activity.setUser(user);
        activity.setAction(action);
        activity.setDetails(details);
        taskActivityRepository.save(activity);
    }
}
