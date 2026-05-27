package com.example.demo.task.repository;

import com.example.demo.task.entity.TaskActivity;
import com.example.demo.security.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    
    Page<TaskActivity> findByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);
    
    Page<TaskActivity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
