# LeaveFlow — Leave Approval System

A production-style **Leave Request & Approval Workflow** built with **Spring Boot 3**, **Camunda 7 BPM Engine**, **PostgreSQL**, and **JWT Security**.

The entire approval lifecycle — from an employee submitting a request to a manager approving/rejecting it to the employee acknowledging the outcome — is orchestrated by a **BPMN 2.0 process** running inside the application. No external workflow tool or manual state tracking is required.

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.5.3 | Application framework |
| Camunda BPM | 7.23.0 | Embedded workflow engine |
| Spring Security + JWT | jjwt 0.11.5 | Authentication & authorisation |
| PostgreSQL | 14+ | Database |
| Spring Data JPA + Hibernate | — | ORM |
| MapStruct | 1.6.3 | Bean mapping |
| Lombok | 1.18.38 | Boilerplate reduction |
| Springdoc / Swagger UI | 2.6.0 | API documentation |
| Maven | Wrapper included | Build tool |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/app/leaveapprovalsystem/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java          # Spring Security + JWT filter chain
│   │   │   ├── DataInitializer.java         # Seeds ADMIN/MANAGER/EMPLOYEE roles on startup
│   │   │   └── SwaggerConfig.java           # OpenAPI / Swagger config
│   │   ├── controller/
│   │   │   ├── AuthController.java          # /api/auth — register, login, me
│   │   │   ├── LeaveController.java         # /api/leaves — apply, approve, reject, notifications
│   │   │   └── UserController.java          # /api/users — user management (admin)
│   │   ├── delegate/                        # Camunda Java Delegates (auto-called by engine)
│   │   │   ├── SaveLeaveDelegate.java       # Creates leave_requests DB row
│   │   │   ├── ApproveLeaveDelegate.java    # Sets status = APPROVED
│   │   │   ├── RejectLeaveDelegate.java     # Sets status = REJECTED
│   │   │   └── NotifyEmployeeDelegate.java  # Sends/logs notification to employee
│   │   ├── dto/                             # Request and Response DTOs
│   │   ├── entity/
│   │   │   ├── User.java                    # Single entity for ALL roles (ADMIN/MANAGER/EMPLOYEE)
│   │   │   ├── LeaveRequest.java            # Leave request entity
│   │   │   ├── Role.java                    # JPA entity → roles table (id, name)
│   │   │   └── RoleName.java                # Enum: ADMIN, MANAGER, EMPLOYEE (compile-time safety)
│   │   ├── exception/                       # Custom exceptions + GlobalExceptionHandler
│   │   ├── mapper/                          # MapStruct mappers (entity → DTO)
│   │   ├── repository/                      # Spring Data JPA repositories
│   │   ├── security/
│   │   │   ├── JwtUtil.java                 # JWT generate/validate/extract
│   │   │   ├── JwtAuthenticationFilter.java # Intercepts every request, validates token
│   │   │   └── CustomUserDetailsService.java
│   │   ├── service/
│   │   │   ├── AuthService.java             # register, login
│   │   │   ├── UserService.java             # CRUD for users
│   │   │   └── LeaveService.java            # Leave workflow + Camunda integration
│   │   └── util/
│   │       └── EmployeeCodeGenerator.java   # Auto-generates employee codes (e.g. EN1001)
│   └── resources/
│       ├── application.yaml                 # All configuration
│       └── processes/
│           └── leave-approval.bpmn          # BPMN 2.0 workflow definition
```

---

## Database Schema

Three application-managed tables are created automatically by Hibernate on first startup.

### roles
Seeded automatically at startup by `DataInitializer`. Never modified at runtime.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | Auto-increment |
| name | VARCHAR(20) UNIQUE | `ADMIN`, `MANAGER`, or `EMPLOYEE` |

### system_users
All users share a **single table**. Profile columns are `NULL` for ADMIN role. `role_id` is a FK → `roles`.

| Column | Type | ADMIN | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| id | BIGINT PK | ✓ | ✓ | ✓ |
| first_name, last_name | VARCHAR | ✓ | ✓ | ✓ |
| email | VARCHAR UNIQUE | ✓ | ✓ | ✓ |
| password | VARCHAR (BCrypt) | ✓ | ✓ | ✓ |
| phone_number | VARCHAR | ✓ | ✓ | ✓ |
| role_id | FK → roles | ✓ | ✓ | ✓ |
| enabled | BOOLEAN | ✓ | ✓ | ✓ |
| employee_code | VARCHAR(20) UNIQUE | NULL | ✓ auto | ✓ auto |
| department | VARCHAR | NULL | ✓ | ✓ |
| designation | VARCHAR | NULL | ✓ | ✓ |
| joining_date | DATE | NULL | ✓ today | ✓ today |
| manager_id | FK → system_users | NULL | NULL | set by Admin |
| created_at | TIMESTAMP | ✓ | ✓ | ✓ |
| updated_at | TIMESTAMP | ✓ | ✓ | ✓ |

### leave_requests
| Column | Type | Notes |
|---|---|---|
| id | BIGINT PK | |
| user_id | FK → system_users | The EMPLOYEE who applied |
| reason | VARCHAR | 10–500 chars |
| days | INT | 1–30 |
| status | VARCHAR | `PENDING` → `APPROVED` or `REJECTED` |
| start_date / end_date | DATE | |
| comments | VARCHAR | Manager's optional note |
| approved_by | VARCHAR | Manager's email |
| notification_sent | BOOLEAN | `true` after employee acknowledges |
| process_instance_id | VARCHAR | Camunda process instance ID |
| applied_at / updated_at | TIMESTAMP | Audit timestamps |

> Camunda also auto-creates ~50 `ACT_*` tables on first startup for its own engine.

---

## Setup & Run

### 1. Create PostgreSQL database
```sql
CREATE DATABASE leave_db;
```

### 2. Configure credentials
Edit `src/main/resources/application.yaml`:
```yaml
spring:
  datasource:
    url:      jdbc:postgresql://localhost:5432/leave_db
    username: postgres       # your PostgreSQL username
    password: yourpassword   # your PostgreSQL password
```

### 3. Run the application
```bash
# Windows
mvnw.cmd spring-boot:run

# Mac / Linux
./mvnw spring-boot:run
```

Hibernate creates all tables automatically on first run.

### 4. Access
| URL | Purpose |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI — interactive API docs |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON spec |

---

## API Quick Reference

### Authentication — `/api/auth`
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public (EMPLOYEE) / ADMIN token (MANAGER, ADMIN) | Register any user |
| POST | `/api/auth/login` | Public | Returns JWT token |
| GET | `/api/auth/me` | Any authenticated | Current user profile |

> **First admin bootstrap:** Call `POST /api/auth/register` with `"role": "ADMIN"` — no token needed when zero admins exist. Once an admin exists, ADMIN token required.

### Users — `/api/users` (Admin only except `/me`)
| Method | Path | Description |
|---|---|---|
| GET | `/api/users` | All users, paginated |
| GET | `/api/users/role/{role}` | Filter by ADMIN / MANAGER / EMPLOYEE |
| GET | `/api/users/{id}` | Get user by ID |
| PUT | `/api/users/{id}` | Update name, phone, department, designation, managerId |
| DELETE | `/api/users/{id}` | Delete user |
| PATCH | `/api/users/{id}/enable` | Enable account |
| PATCH | `/api/users/{id}/disable` | Disable account |
| GET | `/api/users/reports/leaves` | All leave requests report |
| GET | `/api/users/me` | Own profile |
| PUT | `/api/users/me` | Update own profile |

### Leave — `/api/leaves`
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/leaves/apply` | EMPLOYEE | Apply → **starts Camunda process** |
| GET | `/api/leaves/my-leaves` | EMPLOYEE | Own leave history |
| GET | `/api/leaves/notifications` | EMPLOYEE | Pending outcome notifications |
| POST | `/api/leaves/acknowledge/{taskId}` | EMPLOYEE | Acknowledge → **closes Camunda process** |
| GET | `/api/leaves/pending-approvals` | MANAGER | Tasks waiting for decision |
| POST | `/api/leaves/approve/{taskId}` | MANAGER | Approve → **resumes Camunda process** |
| POST | `/api/leaves/reject/{taskId}` | MANAGER | Reject → **resumes Camunda process** |
| GET | `/api/leaves/team-leaves` | MANAGER | Team leave history |

---

## Registration Rules

| role value | Token required | Condition |
|---|---|---|
| `EMPLOYEE` | None — public | Always allowed. Requires `department` and `designation`. |
| `ADMIN` | None on first call · ADMIN token after that | If no admin exists in DB → allowed. Once one exists → ADMIN token required. |
| `MANAGER` | ADMIN token always | Requires `department` and `designation`. |

---

## Complete Usage Flow

### Step 1 — Create the first Admin
```http
POST /api/auth/register
Content-Type: application/json

{
  "firstName":   "Super",
  "lastName":    "Admin",
  "email":       "admin@company.com",
  "password":    "Admin@1234",
  "phoneNumber": "9999999999",
  "role":        "ADMIN"
}
```

### Step 2 — Login as Admin, save the token
```http
POST /api/auth/login
{ "email": "admin@company.com", "password": "Admin@1234" }
```

### Step 3 — Create a Manager (Admin token required)
```http
POST /api/auth/register
Authorization: Bearer <admin-token>

{
  "firstName": "Alice", "lastName": "Manager",
  "email": "alice@company.com", "password": "Manager@1234",
  "phoneNumber": "8888888888", "role": "MANAGER",
  "department": "Engineering", "designation": "Engineering Lead"
}
```

### Step 4 — Create an Employee (public, no token)
```http
POST /api/auth/register

{
  "firstName": "Bob", "lastName": "Employee",
  "email": "bob@company.com", "password": "Employee@1234",
  "phoneNumber": "7777777777", "role": "EMPLOYEE",
  "department": "Engineering", "designation": "Software Engineer"
}
```

### Step 5 — Assign Manager to Employee (Admin token, use IDs from register responses)
```http
PUT /api/users/{employeeId}
Authorization: Bearer <admin-token>

{ "managerId": 2 }
```
> ⚠️ **Required before applying leave.** An employee without a manager cannot have their leave routed for approval.

### Step 6 — Employee applies for leave → Camunda starts
```http
POST /api/leaves/apply
Authorization: Bearer <employee-token>

{
  "reason": "Medical appointment",
  "days": 3,
  "startDate": "2025-02-10",
  "endDate": "2025-02-12"
}
```
Response includes `taskId` — the Camunda `managerApproval` task ID.

> **Camunda auto-runs:** `SaveLeaveDelegate` creates the DB row with `status=PENDING`. Process pauses at manager approval task.

### Step 7 — Manager views pending approvals
```http
GET /api/leaves/pending-approvals
Authorization: Bearer <manager-token>
```
Returns list of tasks with `taskId` values.

### Step 8 — Manager approves or rejects → Camunda resumes
```http
# Approve
POST /api/leaves/approve/{taskId}
Authorization: Bearer <manager-token>
{ "comments": "Approved. Get well soon." }

# OR Reject
POST /api/leaves/reject/{taskId}
Authorization: Bearer <manager-token>
{ "comments": "Short-staffed this week." }
```

> **Camunda auto-runs:** `ApproveLeaveDelegate` or `RejectLeaveDelegate` updates DB status. `NotifyEmployeeDelegate` logs the notification. Process pauses at employee acknowledgement task.

### Step 9 — Employee checks notifications
```http
GET /api/leaves/notifications
Authorization: Bearer <employee-token>
```
Returns `taskId` for acknowledgement.

### Step 10 — Employee acknowledges → Camunda ends
```http
POST /api/leaves/acknowledge/{taskId}
Authorization: Bearer <employee-token>
```

> **Camunda auto-runs:** Process reaches End Event. Instance closed and moved to history.

---

## Camunda BPMN Workflow

The workflow is defined in `src/main/resources/processes/leave-approval.bpmn` and auto-deployed on startup.

```
[START]
   │
   ▼
saveLeaveTask          ← AUTO  (SaveLeaveDelegate)    — creates DB row, status=PENDING
   │
   ▼
managerApproval        ← WAIT  (User Task)            — POST /leaves/approve or /reject
   │
   ▼
<Approved?>
   ├── YES → approveLeaveTask   ← AUTO (ApproveLeaveDelegate)  — status=APPROVED
   └── NO  → rejectLeaveTask    ← AUTO (RejectLeaveDelegate)   — status=REJECTED
                │ (merge)
                ▼
   notifyEmployeeService        ← AUTO (NotifyEmployeeDelegate) — logs notification
                │
                ▼
   notifyEmployee               ← WAIT (User Task)             — POST /leaves/acknowledge
                │
                ▼
              [END]
```

| Step | Type | Triggered by |
|---|---|---|
| saveLeaveTask | Service Task — AUTO | `POST /api/leaves/apply` |
| managerApproval | User Task — **WAIT** | `POST /api/leaves/approve/{taskId}` or `/reject/{taskId}` |
| approveLeaveTask / rejectLeaveTask | Service Task — AUTO | Manager's decision |
| notifyEmployeeService | Service Task — AUTO | Immediately after approve/reject |
| notifyEmployee | User Task — **WAIT** | `POST /api/leaves/acknowledge/{taskId}` |

---

## Password Rules

All passwords must meet:
- Minimum **8 characters**
- At least **1 uppercase letter**
- At least **1 digit**
- At least **1 special character** from `@#$%^&+=!`

Example: `Admin@1234`

---

## Error Responses

All errors follow a consistent envelope:
```json
{
  "success": false,
  "message": "Descriptive error message",
  "timestamp": "2025-01-01T10:00:00"
}
```

| HTTP Status | Scenario |
|---|---|
| 400 | Validation failure, duplicate email, missing required field, invalid role |
| 401 | Wrong email or password at login |
| 403 | Registering MANAGER/ADMIN without correct token, accessing unauthorised route |
| 404 | User / Leave / Camunda task ID not found |
| 500 | Unhandled server error (full stack in logs) |

---

## Key Concepts

**Why Camunda?**
A leave approval is a *long-running, multi-step, stateful workflow* — the manager may act hours or days after the employee applies. Camunda persists the workflow state to the database so the process survives server restarts and is never lost. Without Camunda, you'd need to manually track `status` fields, write state machine logic, and build your own task inbox.

**Single User Table**
All roles (ADMIN, MANAGER, EMPLOYEE) are stored in one `system_users` table. Profile columns (`department`, `designation`, `employee_code`, `manager_id`) are `NULL` for ADMIN and populated for EMPLOYEE/MANAGER at registration time.

**JWT Authentication**
Every request (except `/api/auth/register` and `/api/auth/login`) requires a `Bearer` token in the `Authorization` header. The token encodes the user's email and is validated by `JwtAuthenticationFilter` on every request.

---

## Dependencies Summary

```xml
camunda-bpm-spring-boot-starter   <!-- Workflow engine (engine only, no web UI) -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
postgresql
mapstruct
lombok
jjwt-api / jjwt-impl / jjwt-jackson
springdoc-openapi-starter-webmvc-ui
```
