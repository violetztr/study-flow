# Task Estimated Minutes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional estimated learning duration field to StudyFlow tasks.

**Architecture:** Store the value as minutes in `tasks.estimated_minutes`. The field flows through `TaskRequest`, `TaskService`, `Task`, `TaskResponse`, frontend API types, task forms, task tables, and documentation.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, Flyway, JUnit/MockMvc, React, TypeScript, Ant Design.

---

### Task 1: Backend Contract And Persistence

**Files:**
- Modify: `backend/src/test/java/com/studyflow/task/TaskControllerTest.java`
- Create: `backend/src/main/resources/db/migration/V2__add_task_estimated_minutes.sql`
- Modify: `backend/src/main/java/com/studyflow/task/Task.java`
- Modify: `backend/src/main/java/com/studyflow/task/dto/TaskRequest.java`
- Modify: `backend/src/main/java/com/studyflow/task/dto/TaskResponse.java`
- Modify: `backend/src/main/java/com/studyflow/task/TaskService.java`

- [x] **Step 1: Write failing backend test**

Add `estimatedMinutes` to task creation JSON and assert `$.data.estimatedMinutes` is returned.

- [x] **Step 2: Run backend task test and verify RED**

Run: `mvn -q -Dtest=TaskControllerTest test`

Expected: FAIL because `estimatedMinutes` is not returned yet.

- [x] **Step 3: Implement backend field**

Add Flyway column, Java entity field, DTO field, and service assignment.

- [x] **Step 4: Run backend task test and verify GREEN**

Run: `mvn -q -Dtest=TaskControllerTest test`

Expected: PASS.

### Task 2: Frontend Form And Display

**Files:**
- Modify: `frontend/src/api/tasks.ts`
- Modify: `frontend/src/pages/TasksPage.tsx`
- Modify: `frontend/src/pages/ProjectDetailPage.tsx`

- [x] **Step 1: Add TypeScript field**

Add `estimatedMinutes?: number | null` to task request and response types.

- [x] **Step 2: Add form input and table columns**

Use Ant Design `InputNumber` for "预计学习时长（分钟）" and display `未设置` when empty.

- [x] **Step 3: Run frontend build**

Run: `npm run build`

Expected: PASS.

### Task 3: Documentation And Full Verification

**Files:**
- Modify: `docs/database.md`
- Modify: `docs/api.md`

- [x] **Step 1: Update docs**

Document `estimated_minutes` in the database design and `estimatedMinutes` in API examples.

- [x] **Step 2: Run full verification**

Run backend tests and frontend build:

```bash
mvn test
npm run build
```

Expected: both pass.
