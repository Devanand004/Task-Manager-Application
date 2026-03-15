# Task Manager API

A modern, full-featured Task Management application built with Spring Boot and vanilla JavaScript. Create, update, delete, and manage tasks with an intuitive interface and powerful REST API.

---

## 🚀 Quick Start

### Prerequisites
- **Java 23+** 
- **Maven 3.6+** (or use included Maven Wrapper)
- **Windows/Linux/Mac**

### Installation & Setup

1. **Navigate to project:**
   ```bash
   cd "Task Manager API\demo"
   ```

2. **Build the project:**
   ```bash
   mvnw.cmd clean install
   ```

3. **Run the application:**
   ```bash
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
   java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8081
   ```

4. **Access the application:**
   - **Frontend:** http://localhost:8081/tasks.html
   - **API:** http://localhost:8081/api/tasks

---

## 📋 Features

✅ **Full CRUD Operations**
- Create new tasks
- Read/View all tasks
- Update existing tasks
- Delete tasks

✅ **Task Management**
- Title & Description
- Due dates
- Status tracking (Pending, In Progress, Completed)
- Timestamps (Created, Updated)

✅ **Modern UI**
- Responsive design (mobile-friendly)
- Color-coded status badges
- Smooth animations
- Professional styling

✅ **REST API**
- Complete RESTful endpoints
- JSON request/response format
- CORS enabled
- Error handling

---

## 📁 Project Structure

```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── controller/
│   │   │   │   ├── HomeController.java
│   │   │   │   └── TaskController.java       # REST API endpoints
│   │   │   ├── model/
│   │   │   │   └── Task.java                 # Task entity
│   │   │   ├── service/
│   │   │   │   └── TaskService.java          # Business logic
│   │   │   ├── repository/
│   │   │   │   └── TaskRepository.java       # Data access
│   │   │   └── DemoApplication.java          # Main app
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── tasks.html                # Frontend HTML
│   │       │   ├── script.js                 # Frontend logic
│   │       │   └── style.css                 # Frontend styling
│   │       └── application.properties        # Configuration
│   └── test/
│       └── java/DemoApplicationTests.java    # Unit tests
├── scripts/
│   └── database.sql                          # Database schema
├── pom.xml                                   # Maven config
├── mvnw / mvnw.cmd                          # Maven Wrapper
└── README.md                                 # This file
```

---

## 🔌 REST API Endpoints

### Get All Tasks
```
GET /api/tasks
```
**Response (200):**
```json
[
  {
    "id": 1,
    "title": "Complete Project",
    "description": "Finish the task manager",
    "status": "IN_PROGRESS",
    "dueDate": "2026-03-20",
    "createdAt": "2026-03-15T22:29:20Z",
    "updatedAt": "2026-03-15T22:29:20Z"
  }
]
```

### Get Task by ID
```
GET /api/tasks/{id}
```

### Create Task
```
POST /api/tasks
Content-Type: application/json

{
  "title": "New Task",
  "description": "Task details",
  "status": "PENDING",
  "dueDate": "2026-04-01"
}
```

### Update Task
```
PUT /api/tasks/{id}
Content-Type: application/json

{
  "title": "Updated Task",
  "description": "Updated details",
  "status": "COMPLETED",
  "dueDate": "2026-04-01"
}
```

### Delete Task
```
DELETE /api/tasks/{id}
```

---

## 🗄️ Database Configuration

**Current Database:** MySQL  
**Alternative:** H2 (in-memory) available in configuration

**Database Credentials:**
- **URL:** `jdbc:mysql://localhost:3306/taskdb`
- **Username:** root
- **Password:** (configured in application.properties)

**Schema:** Auto-created by Hibernate (DDL auto-update)

---

## ⚙️ Technology Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | HTML5, CSS3, Vanilla JavaScript |
| **Backend** | Spring Boot 3.5.4 |
| **Framework** | Spring Data JPA, Hibernate |
| **Database** | MySQL 8.0 |
| **Build Tool** | Maven 3.x |
| **Runtime** | Java 23 |

---

## 🛠️ Development

### Build Project
```bash
mvnw.cmd clean install
```

### Run Tests
```bash
mvnw.cmd test
```

### Clean Build
```bash
mvnw.cmd clean
```

---

## 🐛 Troubleshooting

### Port 8081 Already in Use
```bash
netstat -ano | findstr :8081
taskkill /PID <PID> /F
```

### JAVA_HOME Not Set
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
```

### Database Connection Error
- Verify MySQL is running
- Check `application.properties` credentials
- Ensure database exists

### Application Won't Start
```bash
# Kill all Java processes
taskkill /F /IM java.exe

# Start fresh
java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8081
```

---

## 📝 Application Properties

**Location:** `src/main/resources/application.properties`

```properties
# Server
server.port=8081

# MySQL Database
spring.datasource.url=jdbc:mysql://localhost:3306/taskdb
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Spring
spring.application.name=Task Manager API
```

---

## 📊 Status Badges

| Status | Color | Description |
|--------|-------|-------------|
| PENDING | 🟠 Orange | Not started |
| IN_PROGRESS | 🔵 Blue | Currently working on |
| COMPLETED | 🟢 Green | Finished |

---

## 🎯 Key Features Implemented

- ✅ Full CRUD API
- ✅ Responsive UI
- ✅ Status tracking with color coding
- ✅ Due date management
- ✅ Edit mode in frontend
- ✅ Delete confirmation dialog
- ✅ Form validation
- ✅ Error handling
- ✅ Timestamp tracking
- ✅ Cross-Origin Resource Sharing (CORS)

---

## 📄 License

This project is provided as-is for educational purposes.

---

## 👨‍💻 Author

Task Manager API - Built with ❤️ using Spring Boot and Vanilla JavaScript

**Last Updated:** March 15, 2026
