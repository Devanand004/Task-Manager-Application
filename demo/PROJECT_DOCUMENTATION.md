# Task Manager API Documentation

**Version:** 2.0.0 (May 2026)  
**Status:** ✅ Production Ready  
**Build:** Verified with Maven

This document describes the architecture, code structure, feature flows, and implementation decisions of the `demo` task manager application.

---

## 1. Application Overview

The `demo` app is a Spring Boot backend for task management with an optional SPA frontend served from `src/main/resources/static/`. It supports:

- JWT authentication and role-based authorization
- Public task browsing without login
- Authenticated CRUD operations for tasks
- Task filtering, search, and pagination
- Task archive and restore workflows
- Activity logging for audit history
- Centralized API response wrapping and error handling

The code is organized by feature area, not by strict technical layer, which improves developer clarity and maintenance.

---

## 2. Core Package Layout

### `com.example.demo.common`
- `ApiResponse.java`: Generic wrapper for all successful API responses.
- `EnumValue.java`: Custom validation annotation for enum-backed String values.

### `com.example.demo.config`
- `ApplicationConfig.java`: Registers `UserDetailsService`, `AuthenticationProvider`, and `PasswordEncoder`.
- `SecurityConfig.java`: Configures Spring Security, CORS, stateless JWT sessions, and route authorization.
- `OpenApiConfig.java`: Enables API documentation with OpenAPI/Swagger.

### `com.example.demo.exception`
- `ApiErrorResponse.java`: Standard format for error payloads.
- `GlobalExceptionHandler.java`: Handles validation, auth, and runtime exceptions uniformly.
- `TaskNotFoundException.java`: Custom exception for task lookup failures.

### `com.example.demo.security`
- `entity/User.java`: User entity implementing `UserDetails`.
- `entity/Role.java`: Role enum with `ROLE_USER` and `ROLE_ADMIN`.
- `repository/UserRepository.java`: User lookup by username.
- `service/JwtService.java`: Token issuance and validation.
- `config/JwtAuthenticationFilter.java`: Extracts JWT and authenticates requests.
- `controller/AuthController.java`: Registration and login endpoints.

### `com.example.demo.task`
- `entity/Task.java`: Task entity with audit timestamps and soft archive flag.
- `entity/TaskActivity.java`: Activity history storage.
- `entity/TaskStatus.java`: Task status enum.
- `entity/TaskPriority.java`: Priority enum.
- `dto/TaskRequestDTO.java`: Task create/update request contract.
- `dto/TaskResponseDTO.java`: Task response payload.
- `dto/UserProfileResponseDTO.java`: Task statistics payload.
- `mapper/TaskMapper.java`: MapStruct mapper between DTO and entity.
- `mapper/TaskMapperUtil.java`: Safe string-to-enum converters.
- `repository/TaskRepository.java`: Task persistence with custom fetch queries.
- `repository/TaskActivityRepository.java`: Query activity history.
- `repository/TaskSpecification.java`: Dynamic filtering predicates.
- `service/TaskService.java`: Business logic, caching, and activity logging.
- `controller/TaskController.java`: Authenticated task API.
- `controller/PublicTaskController.java`: Public read-only task API.

---

## 3. Authentication and Security Flow

### 3.1 Registration and Login
- `AuthController.register()` creates users using `ROLE_USER` only.
- `AuthController.login()` authenticates credentials and returns a JWT.
- Passwords are hashed with `BCryptPasswordEncoder(12)`.

### 3.2 Token Handling
- `JwtService` generates tokens signed with a Base64 secret.
- Tokens expire after `security.jwt.expiration` milliseconds.
- `JwtAuthenticationFilter` protects API requests by validating the JWT.

### 3.3 Security Configuration
- `SecurityConfig` disables CSRF and uses stateless sessions.
- Public routes are explicitly permitted for auth, docs, static assets, and public tasks.
- All other requests require authentication.
- CORS allows all origins and common HTTP methods.

---

## 4. Task Management Logic

### 4.1 Public vs Protected APIs
- `PublicTaskController` exposes read-only public endpoints.
- `TaskController` exposes protected CRUD and lifecycle endpoints.

### 4.2 Task Ownership and Authorization
- `getTasks()` scopes results to the authenticated user unless the user is an admin.
- `getTaskById()`, `updateTask()`, `archiveTask()`, `restoreTask()`, and `deleteTask()` use a `TaskSecurity` bean to enforce ownership or admin access.

### 4.3 Validation and Mapping
- `TaskRequestDTO` validates all incoming request fields.
- `TaskMapper` uses `TaskMapperUtil` to convert status and priority from strings to enums.
- Invalid string values default to safe defaults rather than throwing runtime errors.

### 4.4 Caching and Performance
- `TaskService.getTaskById()` is `@Cacheable`.
- `createTask()`, `updateTask()`, `archiveTask()`, and `restoreTask()` are `@CachePut` so that cache entries stay fresh.
- `deleteTask()` uses `@CacheEvict`.

### 4.5 Task Stats
- `TaskController.getStats()` returns a `UserProfileResponseDTO` with total, completed, pending, overdue task counts, and user role.

---

## 5. Data Access and Filtering

### 5.1 Repository Support
- `TaskRepository` extends `JpaSpecificationExecutor<Task>` for flexible queries.
- Provides custom fetch joins for user associations.
- Includes SQL-level queries for active tasks and due-date checks.

### 5.2 Specification Patterns
- Filters are composed in `TaskService.getTasks()` using `Specification.allOf(...)`.
- Available filters include user, status, priority, category, archived, due date, date range, and keyword search.

---

## 6. API Contracts

### 6.1 Successful Response Format
All successful responses are wrapped with `ApiResponse<T>`:
```json
{
  "success": true,
  "message": "Tasks retrieved successfully",
  "data": { ... }
}
```

### 6.2 Error Response Format
Error responses use `ApiErrorResponse` with details about the failure.

---

## 7. Database and Migration

- Schema is versioned using Flyway in `src/main/resources/db/migration/V1__init_schema.sql`.
- Task timestamps are managed with `@PrePersist` and `@PreUpdate`.
- The `archived` boolean flag supports soft delete.
- Users are persisted in `users` table; tasks in `tasks`; activity records in `task_activities`.

---

## 8. Build and Run

### 8.1 Local Build
```powershell
cd "d:\Task Manager API\demo"
.\mvnw.cmd clean install
```

### 8.2 Run Test Profile
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test
```

### 8.3 Run Dev Profile
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### 8.4 Docker Compose
```powershell
docker-compose up --build
```

---

## 9. Notes for Developers

- The effective application lives in `demo/`.
- `target/` is generated build output and should be ignored.
- Use `application-test.yml` for fast local development without MySQL.
- Use `application-dev.yml` for local MySQL debugging.
- Use `application-prod.yml` for production-grade configuration.

---

## 10. Recommended Review Points

- `SecurityConfig` route rules and CORS configuration
- `JwtAuthenticationFilter` token validation safety
- `TaskService` cache annotations and activity logging
- `TaskSpecification` filter composition
- `TaskRequestDTO` and `TaskMapper` validation/mapping behavior

---

## 11. Summary
This documentation now matches the actual `demo` project code and explains the major modules, security model, task workflows, API contracts, and runtime configuration clearly for developer onboarding or maintenance review.
