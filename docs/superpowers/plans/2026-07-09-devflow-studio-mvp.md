# DevFlow Studio MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade StudyFlow into the first usable DevFlow Studio MVP: a personal engineering command center with richer project profiles, GitHub repository metadata, and a public portfolio page.

**Architecture:** Keep the current modular monolith. Extend the existing Spring Boot project module, add GitHub and portfolio boundaries, keep current StudyFlow learning features working, and expose a public portfolio API that does not require login.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Flyway, MySQL, H2 tests, React, TypeScript, Vite, Ant Design, TanStack Query, Docker Compose, Nginx.

---

## File Structure

Backend files to create:

- `backend/src/main/resources/db/migration/V5__add_devflow_project_hub.sql`
- `backend/src/main/java/com/studyflow/project/profile/ProjectProfile.java`
- `backend/src/main/java/com/studyflow/project/profile/ProjectProfileMapper.java`
- `backend/src/main/java/com/studyflow/project/profile/ProjectTechStack.java`
- `backend/src/main/java/com/studyflow/project/profile/ProjectTechStackMapper.java`
- `backend/src/main/java/com/studyflow/project/profile/PortfolioProject.java`
- `backend/src/main/java/com/studyflow/project/profile/PortfolioProjectMapper.java`
- `backend/src/main/java/com/studyflow/project/profile/ProjectHubService.java`
- `backend/src/main/java/com/studyflow/project/profile/ProjectHubController.java`
- `backend/src/main/java/com/studyflow/project/profile/dto/ProjectProfileRequest.java`
- `backend/src/main/java/com/studyflow/project/profile/dto/ProjectProfileResponse.java`
- `backend/src/main/java/com/studyflow/project/profile/dto/ProjectTechStackRequest.java`
- `backend/src/main/java/com/studyflow/project/profile/dto/ProjectTechStackResponse.java`
- `backend/src/main/java/com/studyflow/project/profile/dto/PortfolioProjectRequest.java`
- `backend/src/main/java/com/studyflow/project/profile/dto/PortfolioProjectResponse.java`
- `backend/src/main/java/com/studyflow/github/GitHubRepository.java`
- `backend/src/main/java/com/studyflow/github/GitHubRepositoryMapper.java`
- `backend/src/main/java/com/studyflow/github/GitHubClient.java`
- `backend/src/main/java/com/studyflow/github/GitHubApiClient.java`
- `backend/src/main/java/com/studyflow/github/GitHubService.java`
- `backend/src/main/java/com/studyflow/github/GitHubController.java`
- `backend/src/main/java/com/studyflow/github/dto/GitHubRepositoryRequest.java`
- `backend/src/main/java/com/studyflow/github/dto/GitHubRepositoryResponse.java`
- `backend/src/main/java/com/studyflow/github/dto/GitHubSyncResult.java`
- `backend/src/main/java/com/studyflow/portfolio/PortfolioController.java`
- `backend/src/main/java/com/studyflow/portfolio/PortfolioService.java`
- `backend/src/main/java/com/studyflow/portfolio/dto/PublicPortfolioProjectResponse.java`
- `backend/src/test/java/com/studyflow/project/ProjectHubControllerTest.java`
- `backend/src/test/java/com/studyflow/github/GitHubControllerTest.java`
- `backend/src/test/java/com/studyflow/portfolio/PortfolioControllerTest.java`

Backend files to modify:

- `backend/src/main/java/com/studyflow/security/SecurityConfig.java`
- `backend/src/main/java/com/studyflow/project/dto/ProjectResponse.java`
- `backend/src/main/java/com/studyflow/project/ProjectService.java`
- `docs/api.md`
- `docs/database.md`
- `README.md`

Frontend files to create:

- `frontend/src/api/projectHub.ts`
- `frontend/src/api/github.ts`
- `frontend/src/api/portfolio.ts`
- `frontend/src/pages/ProjectHubPage.tsx`
- `frontend/src/pages/PublicPortfolioPage.tsx`
- `frontend/src/pages/PublicProjectDetailPage.tsx`
- `frontend/src/components/project-hub/ProjectProfileForm.tsx`
- `frontend/src/components/project-hub/TechStackEditor.tsx`
- `frontend/src/components/project-hub/GitHubRepositoryPanel.tsx`
- `frontend/src/components/project-hub/PortfolioSettingsCard.tsx`

Frontend files to modify:

- `frontend/src/App.tsx`
- `frontend/src/layouts/AppLayout.tsx`
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/pages/ProjectsPage.tsx`
- `frontend/src/api/projects.ts`
- `frontend/src/index.css`

## Data Model

Create these tables in `V5__add_devflow_project_hub.sql`:

```sql
CREATE TABLE project_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    headline VARCHAR(160),
    production_url VARCHAR(300),
    api_doc_url VARCHAR(300),
    database_doc_url VARCHAR(300),
    architecture_summary TEXT,
    interview_highlights TEXT,
    cover_image_url VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_profiles_project_id (project_id),
    INDEX idx_project_profiles_user_id (user_id)
);

CREATE TABLE project_tech_stacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_tech_stacks_project_id (project_id),
    INDEX idx_project_tech_stacks_user_id (user_id)
);

CREATE TABLE github_repositories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    owner VARCHAR(80) NOT NULL,
    repo VARCHAR(120) NOT NULL,
    html_url VARCHAR(300),
    description VARCHAR(500),
    default_branch VARCHAR(80),
    primary_language VARCHAR(80),
    stars INT NOT NULL DEFAULT 0,
    forks INT NOT NULL DEFAULT 0,
    open_issues INT NOT NULL DEFAULT 0,
    pushed_at DATETIME,
    last_synced_at DATETIME,
    readme_present BOOLEAN NOT NULL DEFAULT FALSE,
    languages_json TEXT,
    latest_commits_json TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_github_repositories_project_id (project_id),
    INDEX idx_github_repositories_user_id (user_id)
);

CREATE TABLE portfolio_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    slug VARCHAR(120) NOT NULL,
    public_visible BOOLEAN NOT NULL DEFAULT FALSE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    public_summary VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portfolio_projects_slug (slug),
    UNIQUE KEY uk_portfolio_projects_project_id (project_id),
    INDEX idx_portfolio_projects_visible (public_visible)
);
```

## API Design

Private authenticated APIs:

```text
GET /api/projects/{id}/profile
PUT /api/projects/{id}/profile
PUT /api/projects/{id}/tech-stacks
PUT /api/projects/{id}/github
POST /api/projects/{id}/github/sync
PUT /api/projects/{id}/portfolio
```

Public APIs:

```text
GET /api/portfolio/projects
GET /api/portfolio/projects/{slug}
```

Public frontend routes:

```text
/portfolio
/portfolio/:slug
```

Private frontend routes:

```text
/project-hub
/projects
/projects/:id
```

## Task 1: Backend Project Hub Schema And Profile API

**Files:**

- Create: `backend/src/main/resources/db/migration/V5__add_devflow_project_hub.sql`
- Create: `backend/src/test/java/com/studyflow/project/ProjectHubControllerTest.java`
- Create backend profile entity, mapper, DTO, service, and controller files listed above.

- [x] **Step 1: Write failing test for project profile upsert**

Add this test method to `ProjectHubControllerTest`:

```java
@Test
void upsertProjectProfileStoresEngineeringMetadata() throws Exception {
    String token = registerAndLogin("hub_profile_user", "hub_profile_user@example.com");
    Long projectId = createProject(token, "DevFlow Studio");

    mockMvc.perform(put("/api/projects/{id}/profile", projectId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "headline": "个人全栈研发中台",
                              "productionUrl": "https://www.violet-surf.com",
                              "apiDocUrl": "https://www.violet-surf.com/doc.html",
                              "databaseDocUrl": "https://github.com/violetztr/study-flow/blob/main/docs/database.md",
                              "architectureSummary": "React + Spring Boot + MySQL + Docker",
                              "interviewHighlights": "GitHub 集成、公开作品集、Docker 部署",
                              "coverImageUrl": "https://example.com/cover.png"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.headline").value("个人全栈研发中台"))
            .andExpect(jsonPath("$.data.productionUrl").value("https://www.violet-surf.com"));

    mockMvc.perform(get("/api/projects/{id}/profile", projectId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.architectureSummary").value("React + Spring Boot + MySQL + Docker"));
}
```

- [x] **Step 2: Run test and verify it fails**

Run:

```powershell
cd backend
mvn -q -Dtest=ProjectHubControllerTest test
```

Expected: FAIL because `/api/projects/{id}/profile` does not exist.

- [x] **Step 3: Add database migration**

Create `V5__add_devflow_project_hub.sql` with the SQL from the Data Model section.

- [x] **Step 4: Implement profile entity, mapper, DTO, service, controller**

Use these method signatures:

```java
public ProjectProfileResponse getProfile(Long userId, Long projectId)
public ProjectProfileResponse upsertProfile(Long userId, Long projectId, ProjectProfileRequest request)
```

Service must verify project ownership by selecting `projects.id` and `projects.user_id`.

- [x] **Step 5: Run profile test and verify it passes**

Run:

```powershell
cd backend
mvn -q -Dtest=ProjectHubControllerTest test
```

Expected: PASS for the profile test.

## Task 2: Backend Tech Stack And Portfolio Settings

**Files:**

- Modify: `backend/src/test/java/com/studyflow/project/ProjectHubControllerTest.java`
- Modify: `backend/src/main/java/com/studyflow/project/profile/ProjectHubService.java`
- Modify: `backend/src/main/java/com/studyflow/project/profile/ProjectHubController.java`

- [x] **Step 1: Write failing test for replacing tech stacks**

Add:

```java
@Test
void replaceTechStacksReturnsOrderedStacks() throws Exception {
    String token = registerAndLogin("hub_stack_user", "hub_stack_user@example.com");
    Long projectId = createProject(token, "DevFlow Studio");

    mockMvc.perform(put("/api/projects/{id}/tech-stacks", projectId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            [
                              { "name": "React", "category": "FRONTEND", "sortOrder": 1 },
                              { "name": "Spring Boot", "category": "BACKEND", "sortOrder": 2 },
                              { "name": "Docker", "category": "DEPLOYMENT", "sortOrder": 3 }
                            ]
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(3)))
            .andExpect(jsonPath("$.data[0].name").value("React"))
            .andExpect(jsonPath("$.data[2].category").value("DEPLOYMENT"));
}
```

- [x] **Step 2: Write failing test for portfolio settings**

Add:

```java
@Test
void upsertPortfolioSettingsMakesProjectPublic() throws Exception {
    String token = registerAndLogin("hub_portfolio_user", "hub_portfolio_user@example.com");
    Long projectId = createProject(token, "DevFlow Studio");

    mockMvc.perform(put("/api/projects/{id}/portfolio", projectId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "slug": "devflow-studio",
                              "publicVisible": true,
                              "featured": true,
                              "displayOrder": 1,
                              "publicSummary": "一个个人全栈研发中台。"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.slug").value("devflow-studio"))
            .andExpect(jsonPath("$.data.publicVisible").value(true));
}
```

- [x] **Step 3: Run tests and verify they fail**

Run:

```powershell
cd backend
mvn -q -Dtest=ProjectHubControllerTest test
```

Expected: FAIL because tech stack and portfolio endpoints do not exist.

- [x] **Step 4: Implement tech stack replacement**

Service signature:

```java
public List<ProjectTechStackResponse> replaceTechStacks(
        Long userId,
        Long projectId,
        List<ProjectTechStackRequest> requests
)
```

Delete old stacks by `project_id`, insert incoming stacks, then return ordered list by `sort_order`.

- [x] **Step 5: Implement portfolio settings upsert**

Service signature:

```java
public PortfolioProjectResponse upsertPortfolioSettings(
        Long userId,
        Long projectId,
        PortfolioProjectRequest request
)
```

Validate that `slug` is lowercase URL-safe with this regex:

```java
"^[a-z0-9]+(?:-[a-z0-9]+)*$"
```

- [x] **Step 6: Run tests and verify they pass**

Run:

```powershell
cd backend
mvn -q -Dtest=ProjectHubControllerTest test
```

Expected: PASS.

## Task 3: Backend GitHub Repository Integration

**Files:**

- Create: `backend/src/test/java/com/studyflow/github/GitHubControllerTest.java`
- Create GitHub files listed in File Structure.

- [x] **Step 1: Write failing test for saving GitHub repository settings**

Add:

```java
@Test
void upsertGitHubRepositoryStoresOwnerAndRepo() throws Exception {
    String token = registerAndLogin("github_user", "github_user@example.com");
    Long projectId = createProject(token, "DevFlow Studio");

    mockMvc.perform(put("/api/projects/{id}/github", projectId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "owner": "violetztr",
                              "repo": "study-flow"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.owner").value("violetztr"))
            .andExpect(jsonPath("$.data.repo").value("study-flow"));
}
```

- [x] **Step 2: Write failing test for GitHub sync**

Use a fake `GitHubClient` bean in the test context:

```java
@TestConfiguration
static class FakeGitHubConfig {
    @Bean
    GitHubClient gitHubClient() {
        return (owner, repo) -> new GitHubSyncResult(
                "https://github.com/violetztr/study-flow",
                "StudyFlow repo",
                "main",
                "Java",
                1,
                0,
                0,
                LocalDateTime.parse("2026-07-09T12:00:00"),
                true,
                "{\\"Java\\":1000}",
                "[{\\"sha\\":\\"abc123\\",\\"message\\":\\"feat: test\\"}]"
        );
    }
}
```

Test:

```java
@Test
void syncGitHubRepositoryUpdatesMetadata() throws Exception {
    String token = registerAndLogin("github_sync_user", "github_sync_user@example.com");
    Long projectId = createProject(token, "DevFlow Studio");
    upsertGitHubRepository(token, projectId, "violetztr", "study-flow");

    mockMvc.perform(post("/api/projects/{id}/github/sync", projectId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.htmlUrl").value("https://github.com/violetztr/study-flow"))
            .andExpect(jsonPath("$.data.primaryLanguage").value("Java"))
            .andExpect(jsonPath("$.data.readmePresent").value(true));
}
```

- [x] **Step 3: Run tests and verify they fail**

Run:

```powershell
cd backend
mvn -q -Dtest=GitHubControllerTest test
```

Expected: FAIL because GitHub endpoints do not exist.

- [x] **Step 4: Implement GitHub client abstraction**

Create interface:

```java
public interface GitHubClient {
    GitHubSyncResult fetchRepository(String owner, String repo);
}
```

Create production implementation with Spring `RestClient` or Java `HttpClient`. It must call:

```text
https://api.github.com/repos/{owner}/{repo}
https://api.github.com/repos/{owner}/{repo}/languages
https://api.github.com/repos/{owner}/{repo}/commits?per_page=5
https://api.github.com/repos/{owner}/{repo}/readme
```

- [x] **Step 5: Implement GitHub service and controller**

Service signatures:

```java
public GitHubRepositoryResponse upsertRepository(Long userId, Long projectId, GitHubRepositoryRequest request)
public GitHubRepositoryResponse syncRepository(Long userId, Long projectId)
```

Sync must update metadata and `lastSyncedAt`.

- [x] **Step 6: Run GitHub tests and verify they pass**

Run:

```powershell
cd backend
mvn -q -Dtest=GitHubControllerTest test
```

Expected: PASS.

## Task 4: Public Portfolio Backend

**Files:**

- Create: `backend/src/test/java/com/studyflow/portfolio/PortfolioControllerTest.java`
- Create portfolio files listed in File Structure.
- Modify: `backend/src/main/java/com/studyflow/security/SecurityConfig.java`

- [x] **Step 1: Write failing test for public portfolio list**

Add:

```java
@Test
void publicPortfolioReturnsVisibleProjectsWithoutLogin() throws Exception {
    String token = registerAndLogin("portfolio_user", "portfolio_user@example.com");
    Long projectId = createProject(token, "DevFlow Studio");
    upsertProfile(token, projectId, "个人全栈研发中台");
    replaceTechStacks(token, projectId, List.of("React", "Spring Boot", "Docker"));
    publishPortfolio(token, projectId, "devflow-studio");

    mockMvc.perform(get("/api/portfolio/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].slug").value("devflow-studio"))
            .andExpect(jsonPath("$.data[0].techStacks", hasSize(3)));
}
```

- [x] **Step 2: Write failing test for public project detail**

Add:

```java
@Test
void publicPortfolioDetailReturnsProjectBySlug() throws Exception {
    String token = registerAndLogin("portfolio_detail_user", "portfolio_detail_user@example.com");
    Long projectId = createProject(token, "DevFlow Studio");
    upsertProfile(token, projectId, "个人全栈研发中台");
    publishPortfolio(token, projectId, "devflow-studio-detail");

    mockMvc.perform(get("/api/portfolio/projects/devflow-studio-detail"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("DevFlow Studio"))
            .andExpect(jsonPath("$.data.headline").value("个人全栈研发中台"));
}
```

- [x] **Step 3: Allow public portfolio APIs**

Modify `SecurityConfig` to permit:

```text
/api/portfolio/**
```

- [x] **Step 4: Implement portfolio query service**

Service signatures:

```java
public List<PublicPortfolioProjectResponse> listPublicProjects()
public PublicPortfolioProjectResponse getPublicProject(String slug)
```

Only return records where `public_visible = true`.

- [x] **Step 5: Run portfolio tests and verify they pass**

Run:

```powershell
cd backend
mvn -q -Dtest=PortfolioControllerTest test
```

Expected: PASS.

## Task 5: Frontend API Layer And Product Rebrand

**Files:**

- Create: `frontend/src/api/projectHub.ts`
- Create: `frontend/src/api/github.ts`
- Create: `frontend/src/api/portfolio.ts`
- Modify: `frontend/src/layouts/AppLayout.tsx`
- Modify: `frontend/src/App.tsx`

- [x] **Step 1: Add frontend API types**

Create functions:

```typescript
export function getProjectProfile(projectId: number)
export function saveProjectProfile(projectId: number, request: ProjectProfileRequest)
export function saveProjectTechStacks(projectId: number, request: ProjectTechStackRequest[])
export function savePortfolioSettings(projectId: number, request: PortfolioProjectRequest)
export function saveGitHubRepository(projectId: number, request: GitHubRepositoryRequest)
export function syncGitHubRepository(projectId: number)
export function listPublicPortfolioProjects()
export function getPublicPortfolioProject(slug: string)
```

- [x] **Step 2: Add routes**

Modify `App.tsx`:

```tsx
<Route path="/project-hub" element={<ProjectHubPage />} />
<Route path="/portfolio" element={<PublicPortfolioPage />} />
<Route path="/portfolio/:slug" element={<PublicProjectDetailPage />} />
```

Keep existing `/projects`, `/notes`, `/daily`, `/tasks`.

- [x] **Step 3: Rebrand app shell**

Modify `AppLayout.tsx` brand:

```tsx
<strong>DevFlow Studio</strong>
<small>个人全栈研发中台</small>
```

Add menu group:

```text
研发中台
  项目中台
  公开作品集
```

- [x] **Step 4: Build frontend**

Run:

```powershell
cd frontend
npm run build
```

Expected: build succeeds.

## Task 6: Frontend Project Hub Page

**Files:**

- Create: `frontend/src/pages/ProjectHubPage.tsx`
- Create: `frontend/src/components/project-hub/ProjectProfileForm.tsx`
- Create: `frontend/src/components/project-hub/TechStackEditor.tsx`
- Create: `frontend/src/components/project-hub/GitHubRepositoryPanel.tsx`
- Create: `frontend/src/components/project-hub/PortfolioSettingsCard.tsx`
- Modify: `frontend/src/index.css`

- [x] **Step 1: Build Project Hub layout**

Layout:

```text
Header: DevFlow Studio Project Hub
Left: project selector
Right top: profile form
Right middle: tech stack editor + GitHub panel
Right bottom: portfolio settings
```

- [x] **Step 2: Profile form fields**

Fields:

```text
headline
productionUrl
apiDocUrl
databaseDocUrl
architectureSummary
interviewHighlights
coverImageUrl
```

- [x] **Step 3: Tech stack editor**

Support adding/removing stack rows:

```text
name
category: FRONTEND / BACKEND / DATABASE / DEPLOYMENT / TOOLING / OTHER
sortOrder
```

- [x] **Step 4: GitHub panel**

Fields:

```text
owner
repo
```

Actions:

```text
Save repository settings
Sync GitHub metadata
```

Display:

```text
stars
forks
primaryLanguage
defaultBranch
readmePresent
lastSyncedAt
```

- [x] **Step 5: Portfolio settings**

Fields:

```text
slug
publicVisible
featured
displayOrder
publicSummary
```

- [x] **Step 6: Build frontend**

Run:

```powershell
cd frontend
npm run build
```

Expected: build succeeds.

## Task 7: Frontend Public Portfolio

**Files:**

- Create: `frontend/src/pages/PublicPortfolioPage.tsx`
- Create: `frontend/src/pages/PublicProjectDetailPage.tsx`
- Modify: `frontend/src/index.css`

- [x] **Step 1: Build public portfolio list page**

Route:

```text
/portfolio
```

Show:

```text
Hero: DevFlow Studio portfolio
Project cards
Tech stack badges
GitHub URL
Production URL
Featured marker
```

- [x] **Step 2: Build public project detail page**

Route:

```text
/portfolio/:slug
```

Show:

```text
project name
headline
public summary
architecture summary
interview highlights
tech stack list
GitHub metadata
online URL
API docs URL
database docs URL
```

- [x] **Step 3: Build frontend**

Run:

```powershell
cd frontend
npm run build
```

Expected: build succeeds.

## Task 8: Documentation, Verification, Commit, Deploy Notes

**Files:**

- Modify: `README.md`
- Modify: `docs/api.md`
- Modify: `docs/database.md`
- Modify: `docs/deploy.md`

- [x] **Step 1: Update README positioning**

Change title to:

```text
DevFlow Studio 个人全栈研发中台
```

Explain that StudyFlow is now the learning-growth module inside DevFlow Studio.

- [x] **Step 2: Update API docs**

Document:

```text
/api/projects/{id}/profile
/api/projects/{id}/tech-stacks
/api/projects/{id}/github
/api/projects/{id}/github/sync
/api/projects/{id}/portfolio
/api/portfolio/projects
/api/portfolio/projects/{slug}
```

- [x] **Step 3: Update database docs**

Document:

```text
project_profiles
project_tech_stacks
github_repositories
portfolio_projects
```

- [x] **Step 4: Run full verification**

Run:

```powershell
cd backend
mvn test
```

Expected:

```text
BUILD SUCCESS
Failures: 0
Errors: 0
```

Run:

```powershell
cd frontend
npm run build
```

Expected:

```text
✓ built
```

- [x] **Step 5: Commit**

Commit in logical chunks:

```bash
git add backend docs README.md
git commit -m "feat: add devflow project hub backend"
git add frontend README.md
git commit -m "feat: add devflow project hub frontend"
```

- [ ] **Step 6: Push**

Run:

```powershell
git push
```

- [x] **Step 7: Server deployment command**

On server:

```bash
cd ~/study-flow
git pull
sudo docker compose up -d --build
sudo docker compose ps
```

Expected: backend starts and Flyway migrates to version 5.
