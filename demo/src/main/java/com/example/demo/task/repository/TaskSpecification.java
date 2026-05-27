package com.example.demo.task.repository;

import com.example.demo.task.entity.Task;
import com.example.demo.task.entity.TaskStatus;
import com.example.demo.task.entity.TaskPriority;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TaskSpecification {

    public static Specification<Task> belongsToUser(Long userId) {
        return (root, query, cb) -> userId == null ? cb.conjunction() : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasCategory(String category) {
        return (root, query, cb) -> (category == null || category.trim().isEmpty()) ? cb.conjunction() : cb.equal(root.get("category"), category);
    }

    public static Specification<Task> isArchived(Boolean archived) {
        return (root, query, cb) -> archived == null ? cb.equal(root.get("archived"), false) : cb.equal(root.get("archived"), archived);
    }

    public static Specification<Task> hasDueDate(LocalDate dueDate) {
        return (root, query, cb) -> dueDate == null ? cb.conjunction() : cb.equal(root.get("dueDate"), dueDate);
    }

    public static Specification<Task> hasDueDateRange(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            } else if (startDate != null && endDate != null) {
                return cb.between(root.get("dueDate"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("dueDate"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("dueDate"), endDate);
            }
        };
    }

    public static Specification<Task> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
