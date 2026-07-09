# DevFlow Studio Product Design

> Status: Draft approved for planning
>
> Date: 2026-07-09
>
> Current project base: StudyFlow

## 1. Product Positioning

DevFlow Studio is a personal full-stack development command center for an engineer who is building real projects, deploying them online, documenting decisions, and preparing a portfolio for interviews.

It upgrades the current StudyFlow project from a learning task manager into a practical engineering platform:

```text
StudyFlow = learning tasks + notes + daily plans
DevFlow Studio = projects + GitHub + deployments + releases + knowledge base + portfolio + learning growth
```

The product should answer one important interview question:

```text
Can this person build, deploy, operate, document, and explain real software end to end?
```

## 2. Target Users

Primary user:

- A self-taught or early-career full-stack developer.
- Has multiple personal projects.
- Wants to track project progress, deployments, notes, releases, and portfolio proof in one place.
- Wants a project that is useful in daily work and impressive in interviews.

Secondary user:

- Interviewers or collaborators who open the public portfolio page.
- They should quickly see what projects exist, what technologies were used, what is deployed, and how the system is engineered.

## 3. Product Tagline

Chinese:

```text
个人全栈研发中台
```

English:

```text
Personal Full-Stack Engineering Command Center
```

## 4. Core Product Modules

### 4.1 Project Hub

Purpose:

Manage every real project as a first-class engineering asset.

Features:

- Project name, description, status, priority, tags.
- Tech stack list, such as Java, React, Docker, MySQL, Redis.
- GitHub repository URL.
- Production URL.
- API documentation URL.
- Deployment environment notes.
- Database design link.
- Project screenshots.
- Interview highlights.

Why it matters:

This turns scattered projects into a professional portfolio database.

### 4.2 GitHub Repository Center

Purpose:

Connect project records with real GitHub repository data.

Features:

- Store GitHub repository owner and name.
- Fetch repository metadata through GitHub API.
- Display stars, forks, default branch, language breakdown.
- Display latest commits.
- Display README status.
- Display last push time.

Why it matters:

This proves the app is not a toy CRUD system. It integrates with an external developer platform.

### 4.3 Deployment Monitor

Purpose:

Track where projects are deployed and whether they are alive.

Features:

- Deployment target, such as server IP, domain, Docker Compose service name, exposed port.
- Health check URL.
- Last health check status.
- Response time.
- Last checked time.
- Manual refresh.

Future features:

- Scheduled health checks.
- Docker container status sync.
- Nginx route map.

Why it matters:

This demonstrates operational thinking: not just writing code, but running software.

### 4.4 Release Center

Purpose:

Record each deployment or version release.

Features:

- Version name.
- Project ID.
- Git commit hash.
- Release summary.
- Change list.
- Deployment result.
- Rollback note.
- Release time.

Why it matters:

This gives the project a professional software delivery story.

### 4.5 Knowledge Base

Purpose:

Upgrade the existing notes module into a project-centered technical knowledge base.

Features:

- Notes can link to projects.
- Note block editor remains.
- Note categories:
  - Requirement notes.
  - API design.
  - Database design.
  - Bug review.
  - Deployment notes.
  - Learning notes.

Why it matters:

It turns "notes" into engineering documentation, not random text storage.

### 4.6 Learning Growth

Purpose:

Keep the useful StudyFlow learning system, but make it secondary to the bigger DevFlow product.

Features:

- Learning projects.
- Tasks.
- Tags.
- Daily plans.
- Habits.
- Journals.
- Time estimates.

Why it matters:

The product still helps the owner grow, but the main interview story becomes engineering operations and portfolio.

### 4.7 Public Portfolio

Purpose:

Generate an external-facing portfolio page from internal project data.

Features:

- Public project cards.
- Project detail page.
- Tech stack badges.
- Online URL.
- GitHub URL.
- Architecture summary.
- Screenshots.
- Interview highlights.
- Release history.

Why it matters:

This is the "wow" surface for interviews.

## 5. Recommended MVP Scope

The first impressive version should not try to build everything. It should focus on three modules:

```text
MVP = Project Hub + GitHub Repository Center + Public Portfolio
```

MVP features:

- Upgrade current `projects` table or add project profile fields.
- Add project tech stacks.
- Add GitHub repository metadata.
- Add manual GitHub sync button.
- Add public portfolio route.
- Add project detail route for interview display.

Not in MVP:

- Real-time Docker monitoring.
- CI/CD automation.
- AI features.
- Multi-user collaboration.
- Payment or SaaS features.

## 6. Architecture Direction

Keep the current modular monolith first:

```text
React frontend
  -> Spring Boot backend
  -> MySQL
  -> Redis
  -> Docker Compose
  -> Nginx
```

Use backend module boundaries:

```text
com.studyflow.project
com.studyflow.github
com.studyflow.deployment
com.studyflow.release
com.studyflow.note
com.studyflow.daily
```

Use API boundaries:

```text
/api/projects
/api/github
/api/deployments
/api/releases
/api/notes
/api/daily
/api/portfolio
```

Future physical microservices can split after the modular monolith is stable:

```text
devflow-web
devflow-project-service
devflow-integration-service
devflow-monitor-service
devflow-portfolio-service
```

## 7. Data Model Direction

Existing tables to keep:

- `users`
- `projects`
- `tasks`
- `tags`
- `task_tags`
- `notes`
- `note_blocks`
- `daily_plans`
- `journals`
- `habits`
- `habit_records`

New tables for MVP:

- `project_profiles`
- `project_tech_stacks`
- `github_repositories`
- `portfolio_projects`

Future tables:

- `deployment_targets`
- `health_checks`
- `releases`
- `release_changes`
- `project_screenshots`

## 8. Interview Highlights

The project should make these points easy to explain:

- Full-stack CRUD is only the base layer.
- GitHub API integration proves third-party API ability.
- Portfolio generation proves product thinking.
- Deployment records prove DevOps awareness.
- Health checks prove operational awareness.
- Modular backend proves architecture awareness.
- Tests and documentation prove engineering discipline.
- Docker and Nginx prove real deployment experience.

## 9. First Development Phase

Phase name:

```text
DevFlow Studio MVP: Project Hub and Portfolio
```

Goal:

Make the app look and behave like a personal engineering platform, not a learning todo app.

Deliverables:

- Rename visible product copy from StudyFlow to DevFlow Studio where appropriate.
- Keep StudyFlow as the learning module name.
- Add richer project profile data.
- Add tech stack management.
- Add GitHub repository metadata fields.
- Add a public portfolio page.
- Update README with the new product positioning.
- Keep existing routes working.

## 10. Success Criteria

The phase is successful when:

- A visitor can open the app and immediately understand it is a personal full-stack engineering command center.
- The owner can create a project with tech stack, GitHub URL, online URL, and highlights.
- The owner can mark a project as portfolio-visible.
- A public portfolio page displays selected projects beautifully.
- Existing learning, notes, and daily features still work.
- Backend tests pass.
- Frontend build passes.
- README explains why the project has interview value.

