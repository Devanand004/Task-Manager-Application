# Task Manager Enterprise API

This repository contains the `demo` Spring Boot task management application, implemented in **Java 17** with **Spring Boot 3.5.4**. It is designed as a production-ready backend with a lightweight Vanilla JavaScript SPA frontend and secure JWT-based authentication.

## ✅ What This Project Includes

- Secure JWT authentication and registration
- Role-based access control (`ROLE_USER`, `ROLE_ADMIN`)
- Public task browsing without authentication
- Authenticated task CRUD operations
- Archive and restore workflows
- Dynamic filtering, search, pagination, and sorting
- Audit-style task activity logging
- Centralized error handling with consistent API responses
- Environment-aware profiles for `test`, `dev`, and `prod`
- Docker and Docker Compose support
- Flyway database migration support

## 📁 Project Structure

```
demo/
├── src/main/java/com/example/demo/
│   ├── common/               # Shared response wrapper and validation helpers
│   ├── config/               # Security, OpenAPI, and bean configuration
│   ├── exception/            # Global REST exception handling
│   ├── security/             # Authentication, users, JWT, and auth controllers
│   ├── task/                 # Task feature module and business logic
│   └── DemoApplication.java  # Main application entry point
├── src/main/resources/       # Config, migrations, static frontend assets
├── src/test/java/            # Unit and integration tests
├── Dockerfile
├── docker-compose.yml
├── mvnw
├── pom.xml
├── README.md
└── PROJECT_DOCUMENTATION.md
```

## 🧠 Architecture Overview

### Feature-Based Modules
The demo app uses a feature-oriented package structure:

- `common` — API envelopes and shared validation annotations
- `config` — security chain, CORS, and OpenAPI setup
- `exception` — unified error handling and response formatting
- `security` — JWT auth, user entity, role-based access
- `task` — task management, DTOs, entity mapping, and service logic

### Key Concepts
- **Public vs Protected API**: `/api/v1/public/**` is open, while `/api/v1/tasks/**` requires authentication.
- **JWT Security**: Stateless token validation in `JwtAuthenticationFilter`.
- **MapStruct Mapping**: `TaskMapper` converts DTOs into entities safely.
- **Soft Archive**: Tasks can be archived/restored instead of deleted immediately.
- **Activity Logging**: `TaskActivity` records all task lifecycle events.

## 🔐 Security Details

### Authentication API
- `POST /api/v1/auth/register` — Register a new user
- `POST /api/v1/auth/login` — Login and receive a JWT

### Role Rules
- `ROLE_USER`: allowed to create, view, update, archive, restore tasks they own.
- `ROLE_ADMIN`: can access all tasks and delete tasks.

### Improvements in Security Layer
- `BCryptPasswordEncoder(12)` for password hashing
- Robust JWT parsing and token validation
- Safe handling of empty or malformed `Authorization` headers
- `UserDetailsService` validates username input before lookup

## 🧩 Task Functionality

### Protected Task Endpoints
- `GET /api/v1/tasks` — Filterable, paginated task list
- `GET /api/v1/tasks/stats` — User task summary
- `GET /api/v1/tasks/{id}` — Task details (owner/admin only)
- `POST /api/v1/tasks` — Create a new task
- `PUT /api/v1/tasks/{id}` — Update a task
- `PUT /api/v1/tasks/{id}/archive` — Archive a task
- `PUT /api/v1/tasks/{id}/restore` — Restore an archived task
- `DELETE /api/v1/tasks/{id}` — Delete a task (admin or owner)

### Public Task Endpoints
- `GET /api/v1/public/tasks` — Public paginated task list
- `GET /api/v1/public/tasks/{id}` — Public task details

### Task DTO Validation
`TaskRequestDTO` enforces:
- required title, status, and priority
- maximum field lengths
- custom enum validation for `status` and `priority`

### Task Filtering
`TaskSpecification` supports:
- task owner filtering
- status, priority, category filters
- archived state filtering
- due date and date range
- title/description keyword search

## 🧪 Development & Run Instructions

### Build and Test
```powershell
cd "d:\Task Manager API\demo"
.\mvnw.cmd clean install
```

### Run with In-Memory Database
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test
```

### Run with MySQL
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run with Docker Compose
```powershell
docker-compose up --build
```

## 📊 Test Summary
- `DemoApplicationTests`
- `TaskControllerTest`
- `TaskServiceTest`

## 🧾 Notes for Developers

- Use `demo/` as the active application module.
- `target/` is generated build output and should not be committed.
- Frontend SPA assets are served from `src/main/resources/static/`.
- `application-test.yml` is the default profile for local development.
- `docker-compose.yml` includes app and MySQL services.

## 📘 Recommended Review Points

- `demo/src/main/java/com/example/demo/config/SecurityConfig.java`
- `demo/src/main/java/com/example/demo/task/service/TaskService.java`
- `demo/src/main/java/com/example/demo/security/config/JwtAuthenticationFilter.java`
- `demo/src/main/java/com/example/demo/task/repository/TaskSpecification.java`
- `demo/src/main/resources/db/migration/V1__init_schema.sql`

## 📌 What Changed
This documentation now reflects the actual project implementation, including service layer behavior, API contract, task filtering logic, authorization rules, and security flow.
