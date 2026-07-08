# StudyFlow Three Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade StudyFlow from a learning task manager into a full-stack learning cockpit with three clear business modules: Study, Notes, and Daily.

**Architecture:** Use a modular monolith first. Keep one Spring Boot backend, one React frontend, one Docker Compose deployment, and one database for now, but design backend packages, database tables, frontend routes, and API paths with future microservice boundaries in mind.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Flyway, MySQL, Redis, JWT, React, TypeScript, Vite, Ant Design, TanStack Query, Docker Compose, Nginx.

---

## 0. Target Product Shape

StudyFlow will become:

```text
StudyFlow 全栈学习驾驶舱
  |
  |-- 学习模块 Study
  |     |-- 项目
  |     |-- 任务
  |     |-- 标签
  |     |-- 学习统计
  |
  |-- 笔记模块 Notes
  |     |-- 笔记页面树
  |     |-- Notion 风格块编辑器
  |     |-- 收藏
  |     |-- 搜索
  |
  |-- 日常模块 Daily
        |-- 今日计划
        |-- 习惯打卡
        |-- 日记
        |-- 日常统计
```

## 1. Module Boundary Design

### Study Module

Current code already covers most of this module.

Backend packages:

```text
com.studyflow.project
com.studyflow.task
com.studyflow.tag
com.studyflow.statistics
```

Future package name can be:

```text
com.studyflow.study.project
com.studyflow.study.task
com.studyflow.study.tag
com.studyflow.study.statistics
```

Frontend routes:

```text
/study
/study/projects
/study/projects/:id
/study/tasks
```

V1 can keep old routes working:

```text
/dashboard
/projects
/projects/:id
/tasks
```

### Notes Module

Backend package:

```text
com.studyflow.note
```

Database tables:

```text
notes
note_blocks
```

Frontend routes:

```text
/notes
/notes/:id
```

### Daily Module

Backend package:

```text
com.studyflow.daily
```

Database tables:

```text
daily_plans
habits
habit_records
journals
```

Frontend routes:

```text
/daily
/daily/plans
/daily/habits
/daily/journal
```

---

## 2. Phase 1: Navigation And Product Shell

**Goal:** Make the app visibly become a three-module cockpit before adding new backend features.

**Files:**
- Modify: `frontend/src/layouts/AppLayout.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/pages/DashboardPage.tsx`
- Modify: `README.md`

### Tasks

- [ ] Rename sidebar structure from flat menu to grouped menu:

```text
驾驶舱
学习
  项目
  任务
笔记
  笔记工作台
日常
  今日计划
设置
  个人资料
```

- [ ] Keep current pages working:

```text
/dashboard
/projects
/tasks
/settings/profile
```

- [ ] Add placeholder routes:

```text
/notes
/daily
```

- [ ] Create placeholder pages:

```text
frontend/src/pages/NotesPage.tsx
frontend/src/pages/DailyPage.tsx
```

- [ ] Dashboard should show three entry cards:

```text
学习模块：项目、任务、学习时长
笔记模块：学习笔记、知识沉淀
日常模块：今日计划、习惯打卡
```

- [ ] Run frontend build:

```powershell
cd frontend
npm run build
```

- [ ] Commit:

```bash
git add frontend/src README.md
git commit -m "feat: add three-module cockpit navigation"
```

---

## 3. Phase 2: Study Module Cleanup

**Goal:** Treat current project/task/tag/statistics features as the Study module without breaking existing behavior.

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/layouts/AppLayout.tsx`
- Modify: `frontend/src/pages/DashboardPage.tsx`
- Modify: `docs/api.md`
- Modify: `docs/database.md`

### Tasks

- [ ] Add route aliases:

```text
/study/projects -> ProjectsPage
/study/projects/:id -> ProjectDetailPage
/study/tasks -> TasksPage
```

- [ ] Keep old route redirects:

```text
/projects -> /study/projects
/tasks -> /study/tasks
```

- [ ] Update frontend copy:

```text
项目管理 -> 学习项目
任务管理 -> 学习任务
学习任务统计 -> 学习模块统计
```

- [ ] Update README to describe Study module:

```text
学习模块用于管理学习项目、任务、标签、预计学习时长和进度统计。
```

- [ ] Run verification:

```powershell
cd frontend
npm run build
```

```powershell
cd backend
mvn test
```

- [ ] Commit:

```bash
git add frontend docs README.md
git commit -m "refactor: organize study features as study module"
```

---

## 4. Phase 3: Notes Module Backend V1

**Goal:** Add database-backed Notion-style note pages and note blocks.

**Backend package:**

```text
backend/src/main/java/com/studyflow/note
```

**Files to create:**

```text
backend/src/main/java/com/studyflow/note/Note.java
backend/src/main/java/com/studyflow/note/NoteBlock.java
backend/src/main/java/com/studyflow/note/NoteMapper.java
backend/src/main/java/com/studyflow/note/NoteBlockMapper.java
backend/src/main/java/com/studyflow/note/NoteService.java
backend/src/main/java/com/studyflow/note/NoteController.java
backend/src/main/java/com/studyflow/note/dto/NoteRequest.java
backend/src/main/java/com/studyflow/note/dto/NoteResponse.java
backend/src/main/java/com/studyflow/note/dto/NoteBlockRequest.java
backend/src/main/java/com/studyflow/note/dto/NoteBlockResponse.java
backend/src/main/resources/db/migration/V3__add_notes.sql
backend/src/test/java/com/studyflow/note/NoteControllerTest.java
```

### Database Tables

`notes`:

```text
id
user_id
parent_id
title
icon
favorite
archived
sort_order
created_at
updated_at
```

`note_blocks`:

```text
id
note_id
user_id
type
content
checked
sort_order
created_at
updated_at
```

### API Endpoints

```text
GET    /api/notes
POST   /api/notes
GET    /api/notes/{id}
PUT    /api/notes/{id}
DELETE /api/notes/{id}
PUT    /api/notes/{id}/blocks
```

### Backend Tasks

- [ ] Write failing test: logged-in user can create a root note.
- [ ] Implement `notes` table migration.
- [ ] Implement `Note` entity and `NoteMapper`.
- [ ] Implement `NoteRequest` and `NoteResponse`.
- [ ] Implement `NoteService.createNote`.
- [ ] Implement `NoteController.createNote`.
- [ ] Run `mvn -q -Dtest=NoteControllerTest test` and make it pass.
- [ ] Write failing test: logged-in user can create a child note.
- [ ] Implement parent note ownership validation.
- [ ] Write failing test: user cannot access another user's note.
- [ ] Implement ownership checks for list/detail/update/delete.
- [ ] Write failing test: note blocks can be saved and returned in order.
- [ ] Implement `note_blocks` table migration.
- [ ] Implement `NoteBlock` entity and mapper.
- [ ] Implement block replace/save operation.
- [ ] Run full backend tests:

```powershell
cd backend
mvn test
```

- [ ] Commit:

```bash
git add backend
git commit -m "feat: add notes backend module"
```

---

## 5. Phase 4: Notes Module Frontend V1

**Goal:** Add a Notion-style notes workspace inside StudyFlow.

**Files to create:**

```text
frontend/src/api/notes.ts
frontend/src/pages/NotesPage.tsx
frontend/src/pages/NoteDetailPage.tsx
frontend/src/components/notes/NoteSidebar.tsx
frontend/src/components/notes/NoteEditor.tsx
frontend/src/components/notes/NoteBlockEditor.tsx
```

### Frontend Tasks

- [ ] Add `notes.ts` API functions:

```text
listNotes
createNote
getNote
updateNote
saveNoteBlocks
deleteNote
```

- [ ] Implement `/notes` page layout:

```text
left: note tree
right: empty guide or selected note
```

- [ ] Implement `/notes/:id` page:

```text
title editor
block editor
auto save status
```

- [ ] Implement block types:

```text
paragraph
heading
todo
quote
code
```

- [ ] Implement auto-save with debounce:

```text
user stops typing for 800ms -> save blocks
```

- [ ] Add search by title in note sidebar.
- [ ] Add favorite toggle.
- [ ] Add archived hide behavior.
- [ ] Run frontend build:

```powershell
cd frontend
npm run build
```

- [ ] Commit:

```bash
git add frontend
git commit -m "feat: add notes workspace frontend"
```

---

## 6. Phase 5: Daily Module Backend V1

**Goal:** Add daily planning and journaling as the first version of the Daily module.

**Backend package:**

```text
backend/src/main/java/com/studyflow/daily
```

**Files to create:**

```text
backend/src/main/java/com/studyflow/daily/DailyPlan.java
backend/src/main/java/com/studyflow/daily/Journal.java
backend/src/main/java/com/studyflow/daily/DailyPlanMapper.java
backend/src/main/java/com/studyflow/daily/JournalMapper.java
backend/src/main/java/com/studyflow/daily/DailyService.java
backend/src/main/java/com/studyflow/daily/DailyController.java
backend/src/main/java/com/studyflow/daily/dto/DailyPlanRequest.java
backend/src/main/java/com/studyflow/daily/dto/DailyPlanResponse.java
backend/src/main/java/com/studyflow/daily/dto/JournalRequest.java
backend/src/main/java/com/studyflow/daily/dto/JournalResponse.java
backend/src/main/resources/db/migration/V4__add_daily.sql
backend/src/test/java/com/studyflow/daily/DailyControllerTest.java
```

### Database Tables

`daily_plans`:

```text
id
user_id
plan_date
title
description
status
created_at
updated_at
```

`journals`:

```text
id
user_id
journal_date
mood
content
created_at
updated_at
```

### API Endpoints

```text
GET  /api/daily/plans?date=2026-07-09
POST /api/daily/plans
PUT  /api/daily/plans/{id}
DELETE /api/daily/plans/{id}
GET  /api/daily/journal?date=2026-07-09
PUT  /api/daily/journal
```

### Backend Tasks

- [ ] Write failing test: create today's plan.
- [ ] Implement `daily_plans` migration.
- [ ] Implement entity, mapper, DTO, service, controller.
- [ ] Write failing test: update plan status.
- [ ] Implement status validation:

```text
TODO
DOING
DONE
```

- [ ] Write failing test: create or update today's journal.
- [ ] Implement `journals` table.
- [ ] Implement journal upsert by user and date.
- [ ] Run backend tests:

```powershell
cd backend
mvn test
```

- [ ] Commit:

```bash
git add backend
git commit -m "feat: add daily backend module"
```

---

## 7. Phase 6: Daily Module Frontend V1

**Goal:** Add a daily cockpit page for today plan and journal.

**Files to create:**

```text
frontend/src/api/daily.ts
frontend/src/pages/DailyPage.tsx
frontend/src/components/daily/DailyPlanList.tsx
frontend/src/components/daily/JournalEditor.tsx
```

### Frontend Tasks

- [ ] Add `daily.ts` API methods.
- [ ] Build `/daily` page with two columns:

```text
left: 今日计划
right: 今日日记
```

- [ ] Add create plan form.
- [ ] Add plan status switch:

```text
待做 -> 进行中 -> 完成
```

- [ ] Add journal editor:

```text
mood select
content textarea
auto save or save button
```

- [ ] Add daily entry card to Dashboard.
- [ ] Run frontend build:

```powershell
cd frontend
npm run build
```

- [ ] Commit:

```bash
git add frontend
git commit -m "feat: add daily cockpit frontend"
```

---

## 8. Phase 7: Cross-Module Dashboard

**Goal:** Make Dashboard summarize Study, Notes, and Daily together.

### Metrics

Study:

```text
totalTasks
completedTasks
totalEstimatedMinutes
completedEstimatedMinutes
```

Notes:

```text
totalNotes
favoriteNotes
updatedNotesThisWeek
```

Daily:

```text
todayPlans
donePlans
hasTodayJournal
```

### Tasks

- [ ] Add notes overview endpoint:

```text
GET /api/notes/statistics/overview
```

- [ ] Add daily overview endpoint:

```text
GET /api/daily/statistics/overview
```

- [ ] Add frontend API files:

```text
frontend/src/api/notesStatistics.ts
frontend/src/api/dailyStatistics.ts
```

- [ ] Update Dashboard to show three module cards:

```text
学习进度
知识沉淀
今日执行
```

- [ ] Run full verification:

```powershell
cd backend
mvn test
```

```powershell
cd frontend
npm run build
```

- [ ] Commit:

```bash
git add backend frontend docs
git commit -m "feat: add cross-module dashboard overview"
```

---

## 9. Phase 8: Microservice-Ready Refactor

**Goal:** Prepare the app for future physical microservice split without actually splitting deployment yet.

### Backend Boundaries

- [ ] Keep `auth` as shared identity boundary.
- [ ] Move study-related packages under conceptual documentation:

```text
study = project + task + tag + statistics
```

- [ ] Make each module own its request/response DTOs.
- [ ] Avoid direct calls between Notes and Daily.
- [ ] Avoid Notes module reading Study tables directly.
- [ ] Avoid Daily module reading Notes tables directly.

### API Boundaries

```text
/api/study/**
/api/notes/**
/api/daily/**
/api/auth/**
```

V1 can keep old paths as compatibility aliases.

### Database Boundaries

```text
study tables: projects, tasks, tags, task_tags
notes tables: notes, note_blocks
daily tables: daily_plans, journals, habits, habit_records
auth tables: users
```

### Documentation

- [ ] Add architecture doc:

```text
docs/architecture.md
```

- [ ] Add module ownership table:

```text
module
tables
routes
backend package
frontend pages
future service name
```

- [ ] Commit:

```bash
git add docs
git commit -m "docs: describe modular monolith architecture"
```

---

## 10. Future Physical Microservices

Do this only after Study, Notes, and Daily are all usable inside the modular monolith.

Future services:

```text
studyflow-auth-service
studyflow-study-service
studyflow-note-service
studyflow-daily-service
studyflow-web
studyflow-gateway
```

Future deployment:

```text
Nginx / gateway
  -> web frontend
  -> auth-service
  -> study-service
  -> note-service
  -> daily-service
  -> MySQL databases
  -> Redis
```

Physical split checklist:

- [ ] Add service-specific Dockerfiles.
- [ ] Add separate Spring Boot apps.
- [ ] Split database schemas.
- [ ] Add service-to-service auth strategy.
- [ ] Add gateway routing.
- [ ] Add centralized logs.
- [ ] Add health checks.
- [ ] Add deployment rollback plan.

Do not start this phase until the modular monolith has stable tests and the user can explain each module boundary.
