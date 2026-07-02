# StudyFlow Design

## Goal

StudyFlow is a portfolio-ready learning task and personal project management system. It should demonstrate a complete Java full-stack workflow: React pages call Spring Boot APIs, Spring Boot persists data in MySQL, Redis supports authentication-related capabilities, Docker packages the services, and GitHub documents the project clearly enough for review and deployment.

## Target User

The primary user is an individual learner who wants to organize learning projects, break them into tasks, track progress, and review basic completion statistics.

## Technology Stack

### Frontend

- React
- TypeScript
- Vite
- Ant Design
- React Router
- TanStack Query
- Axios

### Backend

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Redis
- JWT authentication
- Knife4j or Swagger for API documentation
- JUnit for tests

### Deployment

- Docker
- Docker Compose
- Nginx
- Linux cloud server

## Version 1 Scope

Version 1 focuses on a complete but intentionally small product. It includes:

- User registration and login
- JWT-based authentication
- Current user profile lookup
- Project creation, listing, editing, and deletion
- Task creation, listing, editing, deletion, and status updates
- Task fields: title, description, status, priority, deadline, and project
- Tag creation and task tagging
- Task filtering by status, priority, project, and keyword
- Statistics overview: total tasks, completed tasks, in-progress tasks, overdue tasks
- API documentation
- Basic backend tests
- README, database design, API notes, and deployment documentation
- Docker-based local and server deployment

The first version does not include team collaboration, notifications, AI features, payment, calendar scheduling, or file attachments. Those features are reserved for future versions after the core product is complete.

## Architecture

The project uses a separated frontend and backend architecture.

- The React frontend is responsible for routing, UI rendering, form handling, and API request state.
- The Spring Boot backend exposes REST APIs, validates input, enforces authentication, and owns business logic.
- MySQL stores users, projects, tasks, tags, and task-tag relationships.
- Redis supports short-lived authentication-related data such as token denylist or verification code storage when those features are added.
- Nginx serves the frontend build and proxies API requests to the backend service.
- Docker Compose starts the application services consistently across local development and deployment.

## Repository Structure

```text
study-flow/
  backend/
  frontend/
  docs/
    database.md
    api.md
    deploy.md
  docs/superpowers/specs/
    2026-07-03-study-flow-design.md
  docker-compose.yml
  README.md
```

## Frontend Design

### Routes

```text
/login
/register
/dashboard
/projects
/projects/:id
/tasks
/settings/profile
```

### Main Views

- Login page: accepts account credentials and stores the returned JWT after login.
- Register page: creates a new user account.
- Dashboard page: shows task totals and progress summary.
- Projects page: lists projects and supports project creation, editing, and deletion.
- Project detail page: shows project information and related tasks.
- Tasks page: shows all tasks with filters for status, priority, project, and keyword.
- Profile page: shows current user information.

### Frontend State

- Server data is managed by TanStack Query.
- Page-level form state is managed by React component state or Ant Design Form.
- Authentication token is stored by a small auth utility and attached to API requests by Axios interceptors.
- Route protection redirects unauthenticated users to `/login`.

## Backend Design

### Layers

- Controller: receives HTTP requests and returns API responses.
- Service: owns business logic and transaction boundaries.
- Mapper: uses MyBatis-Plus to access database tables.
- Entity: maps database tables.
- DTO: receives request bodies and returns response data.
- Security: validates JWT and resolves the current user.
- Common: shared response format, exception handling, and validation errors.

### API Response Shape

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Errors use a non-zero code and a clear message.

## Database Design

### users

Stores application users.

- id
- username
- email
- password_hash
- created_at
- updated_at

### projects

Stores user-owned learning projects.

- id
- user_id
- name
- description
- status
- created_at
- updated_at

### tasks

Stores tasks under projects.

- id
- user_id
- project_id
- title
- description
- status
- priority
- deadline
- completed_at
- created_at
- updated_at

### tags

Stores user-owned tags.

- id
- user_id
- name
- color
- created_at
- updated_at

### task_tags

Stores many-to-many relationships between tasks and tags.

- id
- task_id
- tag_id
- created_at

## API Design

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/users/me
```

### Projects

```text
GET    /api/projects
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

### Tasks

```text
GET    /api/tasks
POST   /api/tasks
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
```

### Tags

```text
GET  /api/tags
POST /api/tags
```

### Statistics

```text
GET /api/statistics/overview
```

## Data Flow

1. The user logs in from the React login page.
2. The frontend sends credentials to `POST /api/auth/login`.
3. The backend validates credentials and returns a JWT.
4. The frontend stores the JWT and attaches it to later requests.
5. Protected backend APIs parse the JWT and resolve the current user.
6. Project, task, tag, and statistics queries only return data owned by the current user.

## Error Handling

- Backend validation failures return clear field-level or message-level errors.
- Unauthorized requests return `401` and the frontend redirects to `/login`.
- Forbidden ownership violations return `403`.
- Missing resources return `404`.
- Unexpected server errors return `500` with a safe user-facing message.
- Frontend forms display validation messages near the relevant fields.
- Frontend request failures display Ant Design messages or alerts.

## Testing Strategy

Version 1 includes focused backend tests:

- Register user successfully
- Reject duplicate username or email
- Login successfully
- Reject wrong password
- Create project for current user
- List only current user's projects
- Create task under current user's project
- Filter tasks by status and project
- Return statistics for current user

Frontend tests are not required for version 1. Manual frontend verification is documented in the README.

## Documentation Deliverables

### README.md

The README explains:

- Project purpose
- Technology stack
- Main features
- Real screenshots after UI completion
- Local development setup
- Test command
- Deployment summary
- Online demo address after deployment

### docs/database.md

The database document explains:

- Table purpose
- Field list
- Relationships between tables
- Suggested indexes

### docs/api.md

The API document links to the generated Knife4j or Swagger page and summarizes the core endpoints.

### docs/deploy.md

The deployment document explains:

- Required server environment
- Docker Compose startup
- Nginx reverse proxy
- Environment variables
- How to update the deployed application

## Deployment Design

Docker Compose runs:

- MySQL
- Redis
- Spring Boot backend
- Nginx serving the React frontend and proxying `/api`

The production flow is:

1. Build the React frontend.
2. Build the Spring Boot backend jar.
3. Build Docker images.
4. Start services with Docker Compose.
5. Visit the configured domain or server IP in a browser.

## Acceptance Criteria

Version 1 is complete when:

- The GitHub repository contains frontend, backend, docs, and deployment files.
- The backend starts locally and connects to MySQL and Redis.
- The frontend starts locally and can call backend APIs.
- A user can register, log in, create projects, create tasks, tag tasks, filter tasks, and view statistics.
- Knife4j or Swagger exposes API documentation.
- Basic backend tests pass.
- README, database design, API notes, and deployment instructions exist.
- Docker Compose can start the deployable application stack.
- The deployed app has an online access address.
