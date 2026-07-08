# Statistics Estimated Minutes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show total estimated learning time and completed estimated learning time on the statistics dashboard.

**Architecture:** Reuse `tasks.estimated_minutes` as the source of truth. The backend statistics overview returns two new minute-based fields, and the frontend Dashboard displays them as additional statistic cards.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, JUnit/MockMvc, React, TypeScript, Ant Design.

---

### Task 1: Backend Statistics Contract

**Files:**
- Modify: `backend/src/test/java/com/studyflow/statistics/StatisticsControllerTest.java`
- Modify: `backend/src/main/java/com/studyflow/statistics/dto/StatisticsOverviewResponse.java`
- Modify: `backend/src/main/java/com/studyflow/statistics/StatisticsService.java`

- [x] **Step 1: Write failing statistics test**

Add `estimatedMinutes` values to test tasks and assert `totalEstimatedMinutes` and `completedEstimatedMinutes`.

- [x] **Step 2: Run statistics test and verify RED**

Run: `mvn -q -Dtest=StatisticsControllerTest test`

Expected: FAIL because the new response fields do not exist yet.

- [x] **Step 3: Implement backend statistics fields**

Add two fields to `StatisticsOverviewResponse` and sum current user's task durations in `StatisticsService`.

- [x] **Step 4: Run statistics test and verify GREEN**

Run: `mvn -q -Dtest=StatisticsControllerTest test`

Expected: PASS.

### Task 2: Dashboard Display

**Files:**
- Modify: `frontend/src/api/statistics.ts`
- Modify: `frontend/src/pages/DashboardPage.tsx`

- [x] **Step 1: Update TypeScript response type**

Add `totalEstimatedMinutes` and `completedEstimatedMinutes`.

- [x] **Step 2: Add Dashboard cards**

Display "预计总时长" and "已完成时长" in minutes.

- [x] **Step 3: Run frontend build**

Run: `npm run build`

Expected: PASS.

### Task 3: Documentation And Verification

**Files:**
- Modify: `docs/api.md`

- [x] **Step 1: Update API docs**

Document the two new statistics fields.

- [x] **Step 2: Run full verification**

Run:

```bash
mvn test
npm run build
```

Expected: both pass.
