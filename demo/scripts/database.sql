-- Create Database
CREATE DATABASE IF NOT EXISTS task_manager_db;
USE task_manager_db;

-- Drop table if exists to start fresh
DROP TABLE IF EXISTS tasks;

-- Create Tasks table
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert Sample Data
INSERT INTO tasks (title, description, status, due_date) VALUES 
('Complete Project Documentation', 'Write comprehensive documentation for the Task Manager API covering all endpoints, authentication (if any), and setup instructions.', 'PENDING', '2026-03-20'),
('Fix Backend Bugs', 'Investigate and resolve the intermittent database connection drops in the Spring Boot application.', 'IN_PROGRESS', '2026-03-18'),
('Design Landing Page UI', 'Create a modern, responsive landing page using HTML, CSS, and plain JavaScript with glassmorphism effects.', 'COMPLETED', '2026-03-14'),
('Optimize SQL Queries', 'Review the JPA repository methods and optimize slow-running queries using indexing or query rewriting.', 'PENDING', '2026-03-25');
