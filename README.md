# 🛠️ ProjectFlow — Backend API

> A RESTful backend built with **Java 17 + Spring Boot 3** featuring JWT authentication, role-based access control, and PostgreSQL persistence.

---

## 📋 Table of Contents

- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Environment Variables](#-environment-variables)
- [Database Schema](#-database-schema)
- [API Reference](#-api-reference)
- [Role-Based Access](#-role-based-access)
- [Running Tests](#-running-tests)
- [Deploy on Railway](#-deploy-on-railway)

---

## 🧰 Tech Stack

| Layer        | Technology                        |
|--------------|-----------------------------------|
| Language     | Java 17                           |
| Framework    | Spring Boot 3.2                   |
| Security     | Spring Security + JWT (jjwt 0.11) |
| Database     | PostgreSQL                        |
| ORM          | Spring Data JPA / Hibernate       |
| Validation   | Jakarta Bean Validation           |
| Build Tool   | Maven                             |
| Deployment   | Railway (Docker)                  |

---

## 📁 Project Structure

```
project-manager-api/
│
├── Dockerfile
├── pom.xml
│
└── src/main/java/project_manager_api/
    │
    ├── ProjectManagerApiApplication.java     ← Entry point
    │
    ├── config/
    │   ├── CorsConfig.java                   ← CORS rules
    │   └── SecurityConfig.java               ← Spring Security + JWT filter
    │
    ├── controller/
    │   ├── AuthController.java               ← POST /api/auth/signup, /login
    │   ├── ProjectController.java            ← CRUD /api/projects
    │   ├── TaskController.java               ← CRUD /api/tasks
    │   ├── DashboardController.java          ← GET /api/dashboard/stats, /overdue
    │   └── UserController.java               ← GET /api/users/me, /by-email
    │
    ├── dto/
    │   ├── LoginRequest.java
    │   ├── SignupRequest.java
    │   ├── ProjectRequest.java
    │   ├── TaskRequest.java
    │   └── response/
    │       ├── AuthResponse.java
    │       ├── ProjectResponse.java
    │       └── TaskResponse.java
    │
    ├── entity/
    │   ├── User.java
    │   ├── Project.java
    │   ├── Task.java
    │   └── ProjectMember.java
    │
    ├── enums/
    │   ├── Role.java                         ← ADMIN | MEMBER
    │   └── TaskStatus.java                   ← TODO | IN_PROGRESS | DONE
    │
    ├── exception/
    │   ├── ApiException.java
    │   ├── GlobalExceptionHandler.java
    │   └── package-info.java
    │
    ├── repository/
    │   ├── UserRepository.java
    │   ├── ProjectRepository.java
    │   ├── TaskRepository.java
    │   └── ProjectMemberRepository.java
    │
    ├── security/
    │   ├── CustomUserDetailsService.java
    │   ├── JwtFilter.java
    │   └── JwtUtil.java
    │
    ├── service/
    │   ├── AuthService.java
    │   ├── ProjectService.java
    │   ├── TaskService.java
    │   └── impl/
    │       ├── AuthServiceImpl.java
    │       ├── ProjectServiceImpl.java
    │       └── TaskServiceImpl.java
    │
    └── util/
        └── AppConstants.java

src/main/resources/
    └── application.properties
```

---

## 🚀 Getting Started

### Prerequisites

| Tool         | Version  |
|--------------|----------|
| Java JDK     | 17+      |
| Maven        | 3.8+     |
| PostgreSQL   | 14+      |

### 1. Clone the repository

```bash
git clone https://github.com/your-username/project-manager-api.git
cd project-manager-api
```

### 2. Create the PostgreSQL database

```sql
CREATE DATABASE project_manager;
```

### 3. Configure environment variables

Create a `.env` file or export these in your shell:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/project_manager
export DB_USER=postgres
export DB_PASS=yourpassword
export JWT_SECRET=your-super-secret-key-at-least-32-chars
```

Or edit `src/main/resources/application.properties` directly for local development.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API will be live at: **`http://localhost:8080`**

> Hibernate will auto-create all tables on first run (`ddl-auto=update`).

---

## ⚙️ Environment Variables

| Variable       | Description                            | Example                                    |
|----------------|----------------------------------------|--------------------------------------------|
| `DATABASE_URL` | Full JDBC URL to your PostgreSQL DB    | `jdbc:postgresql://localhost:5432/proj_db` |
| `DB_USER`      | Database username                      | `postgres`                                 |
| `DB_PASS`      | Database password                      | `secret123`                                |
| `JWT_SECRET`   | Secret key for signing JWT tokens      | `myVeryLongSecretKey1234567890abcdef`      |

---

## 🗄️ Database Schema

```sql
-- Users
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Projects
CREATE TABLE projects (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    owner_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Project Members (junction table with role)
CREATE TABLE project_members (
    id            BIGSERIAL PRIMARY KEY,
    project_id    BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id       BIGINT NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    role          VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    UNIQUE (project_id, user_id)
);

-- Tasks
CREATE TABLE tasks (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    due_date      DATE,
    project_id    BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    assignee_id   BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 📡 API Reference

### 🔐 Auth

| Method | Endpoint           | Auth | Description              |
|--------|--------------------|------|--------------------------|
| POST   | `/api/auth/signup` | ❌   | Register a new user      |
| POST   | `/api/auth/login`  | ❌   | Login and receive JWT    |

**Signup Request Body**
```json
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "password": "secret123",
  "role": "ADMIN"
}
```

**Login Request Body**
```json
{
  "email": "alice@example.com",
  "password": "secret123"
}
```

**Auth Response**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "name": "Alice Smith",
  "email": "alice@example.com",
  "role": "ADMIN"
}
```

---

### 📁 Projects

> All endpoints require `Authorization: Bearer <token>` header.

| Method | Endpoint                           | Role        | Description              |
|--------|------------------------------------|-------------|--------------------------|
| GET    | `/api/projects`                    | Any Member  | Get all my projects      |
| GET    | `/api/projects/{id}`               | Member      | Get project by ID        |
| POST   | `/api/projects`                    | Any         | Create a new project     |
| PUT    | `/api/projects/{id}`               | Admin       | Update project details   |
| DELETE | `/api/projects/{id}`               | Owner       | Delete project           |
| POST   | `/api/projects/{id}/members`       | Admin       | Add member to project    |
| DELETE | `/api/projects/{id}/members/{uid}` | Admin       | Remove member            |

**Create/Update Project Body**
```json
{
  "name": "Website Redesign",
  "description": "Redesign the company website Q3"
}
```

**Add Member Body**
```json
{
  "email": "bob@example.com",
  "role": "MEMBER"
}
```

---

### ✅ Tasks

| Method | Endpoint                          | Role              | Description              |
|--------|-----------------------------------|-------------------|--------------------------|
| GET    | `/api/tasks/my`                   | Any               | Get my assigned tasks    |
| GET    | `/api/tasks/overdue`              | Any               | Get my overdue tasks     |
| GET    | `/api/tasks/project/{projectId}`  | Member            | Get tasks by project     |
| POST   | `/api/tasks`                      | Admin             | Create task              |
| PUT    | `/api/tasks/{id}`                 | Admin             | Update task              |
| PATCH  | `/api/tasks/{id}/status?status=`  | Assignee or Admin | Update task status only  |
| DELETE | `/api/tasks/{id}`                 | Admin             | Delete task              |

**Create/Update Task Body**
```json
{
  "title": "Design homepage mockup",
  "description": "Create Figma mockup for homepage",
  "status": "TODO",
  "dueDate": "2025-06-15",
  "projectId": 1,
  "assigneeId": 3
}
```

**Task Status Values:** `TODO` | `IN_PROGRESS` | `DONE`

---

### 📊 Dashboard

| Method | Endpoint                  | Description                       |
|--------|---------------------------|-----------------------------------|
| GET    | `/api/dashboard/stats`    | Task counts by status for user    |
| GET    | `/api/dashboard/overdue`  | All overdue tasks for user        |

**Stats Response**
```json
{
  "TODO": 5,
  "IN_PROGRESS": 3,
  "DONE": 12,
  "OVERDUE": 2
}
```

---

### 👤 Users

| Method | Endpoint                      | Description                |
|--------|-------------------------------|----------------------------|
| GET    | `/api/users/me`               | Get current user profile   |
| GET    | `/api/users/by-email?email=`  | Find user by email         |

---

## 🔐 Role-Based Access

```
ADMIN  → Can create/update/delete projects and tasks, manage members
MEMBER → Can view projects/tasks, update status of assigned tasks
Owner  → Only project owner can delete the project
```

| Action                   | ADMIN | MEMBER | Assignee |
|--------------------------|-------|--------|----------|
| Create project           | ✅    | ✅     | -        |
| Update/Delete project    | ✅    | ❌     | -        |
| Add/Remove members       | ✅    | ❌     | -        |
| Create/Delete tasks      | ✅    | ❌     | -        |
| Update task status       | ✅    | ❌     | ✅       |
| View project tasks       | ✅    | ✅     | ✅       |

---

## 🧪 Running Tests

```bash
# Run all tests
./mvnw test

# Run with coverage report
./mvnw test jacoco:report
```

---

## 🚀 Deploy on Railway

### Step 1 — Push to GitHub

```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/your-username/project-manager-api.git
git push -u origin main
```

### Step 2 — Create Railway Project

1. Go to [railway.app](https://railway.app) → **New Project**
2. Click **Add Service → GitHub Repo** → select your backend repo
3. Click **Add Service → Database → PostgreSQL** (Railway sets `DATABASE_URL` automatically)

### Step 3 — Set Environment Variables

In Railway → your backend service → **Variables**:

```
DB_USER      = (from Railway PostgreSQL plugin)
DB_PASS      = (from Railway PostgreSQL plugin)
JWT_SECRET   = your-random-secret-min-32-chars
FRONTEND_URL = https://your-frontend.railway.app
```

### Step 4 — Deploy

Railway auto-detects the `Dockerfile` and builds/deploys automatically on every push.

Your API will be live at: `https://project-manager-api.up.railway.app`

---

## 🐳 Docker (Local)

```bash
# Build image
docker build -t project-manager-api .

# Run container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/project_manager \
  -e DB_USER=postgres \
  -e DB_PASS=yourpassword \
  -e JWT_SECRET=your-secret-key \
  project-manager-api
```

---

## 🛡️ Security Notes

- Passwords are hashed with **BCrypt** (never stored in plain text)
- JWT tokens expire after **24 hours**
- All endpoints except `/api/auth/**` require a valid JWT
- CORS is configured to allow only your frontend origin

---

## 📬 Error Response Format

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2025-06-10T14:32:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already in use"
}
```

| Status | Meaning                        |
|--------|--------------------------------|
| 400    | Bad request / validation error |
| 401    | Unauthorized (invalid/missing JWT) |
| 403    | Forbidden (insufficient role)  |
| 404    | Resource not found             |
| 500    | Internal server error          |

---

## 👨‍💻 Author

Built with ☕ and Java — ready for production deployment on Railway.
