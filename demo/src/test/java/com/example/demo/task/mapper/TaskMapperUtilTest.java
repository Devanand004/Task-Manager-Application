package com.example.demo.task.mapper;

import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.entity.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskMapperUtil — the helper that converts raw strings
 * into TaskStatus and TaskPriority enums.
 *
 * WHY this matters:
 *   Invalid or unexpected string inputs (e.g. "pending" lowercase, empty string, null)
 *   must be handled safely and predictably. These tests prevent silent data corruption
 *   where the wrong enum value gets saved to the database.
 */
@DisplayName("TaskMapperUtil Unit Tests")
class TaskMapperUtilTest {

    private TaskMapperUtil mapperUtil;

    @BeforeEach
    void setUp() {
        mapperUtil = new TaskMapperUtil();
    }

    // ============================================================
    // stringToTaskStatus Tests
    // ============================================================

    @Nested
    @DisplayName("stringToTaskStatus")
    class StringToTaskStatus {

        @Test
        @DisplayName("Converts 'PENDING' to TaskStatus.PENDING")
        void convertsUpperCasePending() {
            assertEquals(TaskStatus.PENDING, mapperUtil.stringToTaskStatus("PENDING"));
        }

        @Test
        @DisplayName("Converts 'IN_PROGRESS' to TaskStatus.IN_PROGRESS")
        void convertsUpperCaseInProgress() {
            assertEquals(TaskStatus.IN_PROGRESS, mapperUtil.stringToTaskStatus("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Converts 'COMPLETED' to TaskStatus.COMPLETED")
        void convertsUpperCaseCompleted() {
            assertEquals(TaskStatus.COMPLETED, mapperUtil.stringToTaskStatus("COMPLETED"));
        }

        @Test
        @DisplayName("Converts lowercase 'pending' to TaskStatus.PENDING")
        void convertsLowerCasePending() {
            // The util is case-insensitive — this is the expected behavior
            assertEquals(TaskStatus.PENDING, mapperUtil.stringToTaskStatus("pending"));
        }

        @Test
        @DisplayName("Converts mixed case 'In_Progress' to TaskStatus.IN_PROGRESS")
        void convertsMixedCaseInProgress() {
            assertEquals(TaskStatus.IN_PROGRESS, mapperUtil.stringToTaskStatus("in_progress"));
        }

        @Test
        @DisplayName("Returns PENDING as default for null input")
        void returnsDefaultForNull() {
            // Null input should safely return a default, not throw an exception
            assertEquals(TaskStatus.PENDING, mapperUtil.stringToTaskStatus(null));
        }

        @Test
        @DisplayName("Returns PENDING as default for empty string input")
        void returnsDefaultForEmptyString() {
            assertEquals(TaskStatus.PENDING, mapperUtil.stringToTaskStatus(""));
        }

        @Test
        @DisplayName("Returns PENDING as default for invalid status string")
        void returnsDefaultForInvalidStatus() {
            assertEquals(TaskStatus.PENDING, mapperUtil.stringToTaskStatus("INVALID_STATUS"));
        }

        @Test
        @DisplayName("Returns PENDING as default for gibberish input")
        void returnsDefaultForGibberish() {
            assertEquals(TaskStatus.PENDING, mapperUtil.stringToTaskStatus("xyz!@#"));
        }
    }

    // ============================================================
    // stringToTaskPriority Tests
    // ============================================================

    @Nested
    @DisplayName("stringToTaskPriority")
    class StringToTaskPriority {

        @Test
        @DisplayName("Converts 'LOW' to TaskPriority.LOW")
        void convertsUpperCaseLow() {
            assertEquals(TaskPriority.LOW, mapperUtil.stringToTaskPriority("LOW"));
        }

        @Test
        @DisplayName("Converts 'MEDIUM' to TaskPriority.MEDIUM")
        void convertsUpperCaseMedium() {
            assertEquals(TaskPriority.MEDIUM, mapperUtil.stringToTaskPriority("MEDIUM"));
        }

        @Test
        @DisplayName("Converts 'HIGH' to TaskPriority.HIGH")
        void convertsUpperCaseHigh() {
            assertEquals(TaskPriority.HIGH, mapperUtil.stringToTaskPriority("HIGH"));
        }

        @Test
        @DisplayName("Converts lowercase 'low' to TaskPriority.LOW")
        void convertsLowerCaseLow() {
            assertEquals(TaskPriority.LOW, mapperUtil.stringToTaskPriority("low"));
        }

        @Test
        @DisplayName("Converts mixed case 'High' to TaskPriority.HIGH")
        void convertsMixedCaseHigh() {
            assertEquals(TaskPriority.HIGH, mapperUtil.stringToTaskPriority("High"));
        }

        @Test
        @DisplayName("Returns MEDIUM as default for null input")
        void returnsDefaultForNull() {
            assertEquals(TaskPriority.MEDIUM, mapperUtil.stringToTaskPriority(null));
        }

        @Test
        @DisplayName("Returns MEDIUM as default for empty string input")
        void returnsDefaultForEmptyString() {
            assertEquals(TaskPriority.MEDIUM, mapperUtil.stringToTaskPriority(""));
        }

        @Test
        @DisplayName("Returns MEDIUM as default for invalid priority string")
        void returnsDefaultForInvalidPriority() {
            assertEquals(TaskPriority.MEDIUM, mapperUtil.stringToTaskPriority("URGENT"));
        }

        @Test
        @DisplayName("Returns MEDIUM as default for gibberish input")
        void returnsDefaultForGibberish() {
            assertEquals(TaskPriority.MEDIUM, mapperUtil.stringToTaskPriority("!!!"));
        }
    }
}
