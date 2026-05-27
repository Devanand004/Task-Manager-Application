package com.example.demo.common.validation;

import com.example.demo.task.entity.TaskPriority;
import com.example.demo.task.entity.TaskStatus;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EnumValueValidator — the custom Jakarta Validation constraint
 * that ensures string values match a valid enum constant.
 *
 * WHY this matters:
 *   This validator gates all task creation/update requests. If it misbehaves,
 *   invalid data (e.g., "URGENT" as a priority) could silently bypass validation
 *   and corrupt the database. These tests permanently lock down its correctness.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnumValueValidator Unit Tests")
class EnumValueValidatorTest {

    private EnumValueValidator validator;

    @Mock
    private EnumValue annotation;

    @Mock
    private ConstraintValidatorContext context;

    // ============================================================
    // Tests for TaskStatus enum validation
    // ============================================================

    @Nested
    @DisplayName("Validating TaskStatus values")
    class TaskStatusValidation {

        @BeforeEach
        void setUp() {
            validator = new EnumValueValidator();
            when(annotation.enumClass()).thenAnswer(inv -> TaskStatus.class);
            validator.initialize(annotation);
        }

        @Test
        @DisplayName("Accepts 'PENDING' as a valid TaskStatus")
        void acceptsPending() {
            assertTrue(validator.isValid("PENDING", context));
        }

        @Test
        @DisplayName("Accepts 'IN_PROGRESS' as a valid TaskStatus")
        void acceptsInProgress() {
            assertTrue(validator.isValid("IN_PROGRESS", context));
        }

        @Test
        @DisplayName("Accepts 'COMPLETED' as a valid TaskStatus")
        void acceptsCompleted() {
            assertTrue(validator.isValid("COMPLETED", context));
        }

        @Test
        @DisplayName("Accepts lowercase 'pending' (case-insensitive matching)")
        void acceptsLowerCasePending() {
            assertTrue(validator.isValid("pending", context));
        }

        @Test
        @DisplayName("Accepts mixed case 'Completed' (case-insensitive matching)")
        void acceptsMixedCaseCompleted() {
            assertTrue(validator.isValid("Completed", context));
        }

        @Test
        @DisplayName("Rejects 'UNKNOWN' as an invalid TaskStatus")
        void rejectsUnknownStatus() {
            assertFalse(validator.isValid("UNKNOWN", context));
        }

        @Test
        @DisplayName("Rejects 'URGENT' as an invalid TaskStatus")
        void rejectsUrgentStatus() {
            assertFalse(validator.isValid("URGENT", context));
        }

        @Test
        @DisplayName("Rejects empty string as invalid")
        void rejectsEmptyString() {
            assertFalse(validator.isValid("", context));
        }

        @Test
        @DisplayName("Accepts null (null is handled by @NotNull separately)")
        void acceptsNull() {
            // null is intentionally valid here — @NotNull handles null rejection
            // This prevents double-validation and lets Jakarta process in order
            assertTrue(validator.isValid(null, context));
        }
    }

    // ============================================================
    // Tests for TaskPriority enum validation
    // ============================================================

    @Nested
    @DisplayName("Validating TaskPriority values")
    class TaskPriorityValidation {

        @BeforeEach
        void setUp() {
            validator = new EnumValueValidator();
            when(annotation.enumClass()).thenAnswer(inv -> TaskPriority.class);
            validator.initialize(annotation);
        }

        @Test
        @DisplayName("Accepts 'LOW' as a valid TaskPriority")
        void acceptsLow() {
            assertTrue(validator.isValid("LOW", context));
        }

        @Test
        @DisplayName("Accepts 'MEDIUM' as a valid TaskPriority")
        void acceptsMedium() {
            assertTrue(validator.isValid("MEDIUM", context));
        }

        @Test
        @DisplayName("Accepts 'HIGH' as a valid TaskPriority")
        void acceptsHigh() {
            assertTrue(validator.isValid("HIGH", context));
        }

        @Test
        @DisplayName("Accepts lowercase 'high' (case-insensitive matching)")
        void acceptsLowerCaseHigh() {
            assertTrue(validator.isValid("high", context));
        }

        @Test
        @DisplayName("Rejects 'CRITICAL' as an invalid TaskPriority")
        void rejectsCritical() {
            assertFalse(validator.isValid("CRITICAL", context));
        }

        @Test
        @DisplayName("Rejects 'URGENT' as an invalid TaskPriority")
        void rejectsUrgent() {
            assertFalse(validator.isValid("URGENT", context));
        }

        @Test
        @DisplayName("Rejects numeric string '1' as an invalid TaskPriority")
        void rejectsNumericString() {
            assertFalse(validator.isValid("1", context));
        }

        @Test
        @DisplayName("Rejects whitespace-only string as invalid")
        void rejectsWhitespace() {
            assertFalse(validator.isValid("   ", context));
        }
    }
}
