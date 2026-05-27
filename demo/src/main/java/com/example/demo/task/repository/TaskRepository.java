package com.example.demo.task.repository;

import com.example.demo.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.user WHERE t.id = :id")
    Optional<Task> findWithUserById(@Param("id") Long id);

    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.archived = false AND t.status != com.example.demo.task.entity.TaskStatus.COMPLETED AND t.dueDate IS NOT NULL AND t.dueDate <= :maxDate")
    java.util.List<Task> findActiveTasksWithUserDueBeforeOrOn(@Param("maxDate") java.time.LocalDate maxDate);
}
