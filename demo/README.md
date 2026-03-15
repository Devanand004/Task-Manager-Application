# Task Manager API

A simple Spring Boot REST API for managing tasks with JPA and H2 in-memory database.

## Features

- Create new tasks
- Retrieve all tasks
- Task management with title, description, and completion status
- H2 in-memory database with console access
- RESTful API endpoints

## Requirements

- Java 23 (or higher)
- Maven 3.6+ (or use Maven Wrapper included)
- Windows/Linux/Mac OS

## Installation

1. Clone or download the project:
   ```bash
   cd "Task Manager API\demo"
   ```

2. Build the project:
   ```bash
   mvnw.cmd clean install
   ```
   (On Linux/Mac, use `./mvnw clean install`)

## Running the Application

### Using Maven Wrapper:
```bash
.\mvnw.cmd spring-boot:run
```

### Using Java directly:
```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Get All Tasks
- **Endpoint:** `GET /api/tasks`
- **Response:** 
  ```json
  [
    {
      "id": 1,
      "title": "Sample Task",
      "description": "Task description",
      "completed": false
    }
  ]
  ```

### Create a Task
- **Endpoint:** `POST /api/tasks`
- **Request Body:**
  ```json
  {
    "title": "New Task",
    "description": "Task description",
    "completed": false
  }
  ```
- **Response:** 
  ```json
  {
    "id": 1,
    "title": "New Task",
    "description": "Task description",
    "completed": false
  }
  ```

## Database

### H2 In-Memory Database
- **Database Name:** taskdb
- **Console URL:** `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:mem:taskdb`
- **Username:** sa
- **Password:** (empty)

The database schema is automatically created using JPA with Hibernate DDL auto-update.

## Project Structure

```
demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── controller/          # REST controllers
│   │   │   │   ├── HomeController.java
│   │   │   │   └── TaskController.java
│   │   │   ├── model/               # Entity classes
│   │   │   │   └── Task.java
│   │   │   ├── repository/          # JPA repositories
│   │   │   │   └── TaskRepository.java
│   │   │   └── DemoApplication.java # Main Spring Boot application
│   │   └── resources/
│   │       ├── static/              # Static files
│   │       │   ├── tasks.html
│   │       │   ├── script.js
│   │       │   └── style.css
│   │       └── application.properties
│   └── test/
│       └── java/                    # Test files
├── pom.xml
├── mvnw / mvnw.cmd                 # Maven Wrapper
└── README.md
```

## Technologies Used

- **Spring Boot 3.5.4** - Web framework
- **Spring Data JPA** - Data persistence
- **Hibernate** - ORM framework
- **H2 Database** - In-memory database
- **Maven** - Build tool
- **Java 23** - Programming language

## Configuration

Application configuration is in `src/main/resources/application.properties`:

```properties
# H2 database config
spring.datasource.url=jdbc:h2:mem:taskdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# H2 console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Testing

Run tests with:
```bash
.\mvnw.cmd test
```

## Build Output

The built JAR file is located at: `target/demo-0.0.1-SNAPSHOT.jar`

## Troubleshooting

### JAVA_HOME not set
If you get "JAVA_HOME environment variable is not defined correctly", set it:
```bash
# PowerShell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"

# Command Prompt
set JAVA_HOME=C:\Program Files\Java\jdk-23
```

### Port 8080 already in use
If port 8080 is already in use, you can change it:
```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar --server.port=8081
```

## License

This project is provided as-is for educational purposes.

## Support

For issues or questions, please check the application logs or review the test class in `src/test/java/`.
