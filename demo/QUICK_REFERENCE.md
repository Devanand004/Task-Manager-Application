# Task Manager API - Quick Reference Guide

**Last Updated:** May 27, 2026  
**Status:** ✅ Production Ready  
**Version:** 2.0.0

---

## 🚀 Quick Start

### Option 1: Run with In-Memory Database
```powershell
cd "d:\Task Manager API\demo"
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test
```

**Open the app:**
- Frontend: http://localhost:8080/tasks.html
- Swagger: http://localhost:8080/swagger-ui/index.html

### Option 2: Run with Docker
```powershell
docker-compose up --build
```

---

## 📋 API Quick Reference

### Auth Endpoints
```bash
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Public Task Endpoints
```bash
GET /api/v1/public/tasks?page=0&size=10
GET /api/v1/public/tasks/{id}
```

### Protected Task Endpoints
```bash
GET /api/v1/tasks
GET /api/v1/tasks/stats
GET /api/v1/tasks/{id}
POST /api/v1/tasks
PUT /api/v1/tasks/{id}
PUT /api/v1/tasks/{id}/archive
PUT /api/v1/tasks/{id}/restore
DELETE /api/v1/tasks/{id}
```

### Example Auth Flow
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"password123"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"password123"}'
```

### Example Protected Request
```bash
curl -X GET http://localhost:8080/api/v1/tasks \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🧰 Project Structure

```
demo/
├── src/main/java/com/example/demo/
│   ├── common/
│   ├── config/
│   ├── exception/
│   ├── security/
│   ├── task/
│   └── DemoApplication.java
├── src/main/resources/
│   ├── db/migration/
│   ├── static/
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── application-test.yml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## 🔐 Database Profiles

### Test Profile (H2)
- No external database required
- Good for local dev and CI

### Dev Profile (MySQL)
```text
Host: localhost
Port: 3306
Database: task_manager_db
Username: root
Password: password
```

### Production Profile
Use environment variables for database settings and the JWT secret.

---

## 🏗️ Important Technical Notes

### Task Validation
- `TaskRequestDTO` requires title, status, and priority
- Uses custom enum validation for status and priority
- Maps to `Task` entity via MapStruct

### Task Lifecycle
- Create and update operations write audit logs
- Archive and restore preserve data instead of deleting records immediately
- Only admins or task owners can modify or delete tasks

### Security
- JWT tokens are validated in `JwtAuthenticationFilter`
- `SecurityConfig` allows public access to auth and public task endpoints
- Protected endpoints require authenticated users

---

## 🛠️ Commands

```powershell
# Build and test
.\mvnw.cmd clean install

# Run tests only
.\mvnw.cmd test

# Run app in test mode
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test

# Build Docker image
docker build -t task-manager-api:latest .

# Run Docker Compose
docker-compose up --build
```

---

## ⚡ Feature Summary

- Public read-only task browsing
- Authenticated task CRUD operations
- Task search, pagination, and filtering
- Archive/restore workflow
- Activity event logging
- JWT auth with role-based access control
- Standardized API response format

---

## 📌 Developer Guidance

- Use the `demo` folder as the active application module
- Keep `target/` ignored and do not commit generated output
- Use `application-test.yml` for fast local development
- Review `SecurityConfig` and `JwtAuthenticationFilter` when changing auth behavior
- Review `TaskService` and `TaskSpecification` for task business and filter logic
