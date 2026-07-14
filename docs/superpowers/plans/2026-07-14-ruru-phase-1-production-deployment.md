# Ruru Phase 1 Production Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy immutable GHCR images safely, with health verification, backup/restore, rollback, and consistent operations documentation.

**Architecture:** Actions builds backend and frontend images with latest and commit-SHA tags. The server pulls the SHA-tagged images through the production Compose overlay, starts without building, waits for all services to be healthy, and then requests the frontend-proxied health URL. Shared Bash code keeps all operational scripts on one Compose path.

**Tech Stack:** GitHub Actions, GHCR, Docker Compose, Bash, MySQL 8, Spring Boot.

---

## File structure

- Create: `scripts/lib/production-compose.sh` — resolve project root, load .env, run production Compose under Docker or sudo.
- Create: `scripts/test-production-ops.sh` — fake-command shell tests for scripts, workflow, and docs.
- Modify: `.github/workflows/cd.yml` — use commit SHA, wait for health, output diagnostics.
- Modify: `scripts/deploy-fast.sh`, `scripts/rollback.sh`, `scripts/backup-mysql.sh`, `scripts/restore-mysql.sh` — use the common helper.
- Modify: `README.md`, `docs/deploy.md`, `docs/operations.md`, `docs/cd.md`, `.env.example` — document one immutable production flow.

### Task 1: Test and implement production Compose helper

**Files:**
- Create: `scripts/lib/production-compose.sh`
- Create: `scripts/test-production-ops.sh`

- [ ] **Step 1: Write the failing test**

Create a Bash harness that creates a temporary fake `docker` executable, records its arguments in `CALL_LOG`, returns success for `docker ps`, then runs:

```bash
source "$ROOT_DIR/scripts/lib/production-compose.sh"
IMAGE_REGISTRY=ghcr.io/example/ruru IMAGE_TAG=abc123 production_compose pull backend frontend
grep -F 'docker compose -f' "$CALL_LOG"
grep -F 'pull backend frontend' "$CALL_LOG"
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `bash scripts/test-production-ops.sh`

Expected: nonzero exit because the helper file does not exist.

- [ ] **Step 3: Implement the minimal helper**

Create a helper with `production_root_dir`, `load_production_env`, and `production_compose`. The last function must run exactly:

```bash
IMAGE_REGISTRY="$image_registry" IMAGE_TAG="$image_tag" docker compose -f "$root_dir/docker-compose.yml" -f "$root_dir/docker-compose.prod.yml" "$@"
```

when Docker is usable, otherwise the same command through `sudo env`. Use the defaults `ghcr.io/violetztr/study-flow` and `latest`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `bash scripts/test-production-ops.sh`

Expected: exit 0 and the fake command log contains both Compose files and `pull backend frontend`.

- [ ] **Step 5: Commit**

```bash
git add scripts/lib/production-compose.sh scripts/test-production-ops.sh
git commit -m "test: cover production compose helper"
```

### Task 2: Use helper in operations and verify health

**Files:**
- Modify: `scripts/deploy-fast.sh`
- Modify: `scripts/rollback.sh`
- Modify: `scripts/backup-mysql.sh`
- Modify: `scripts/restore-mysql.sh`
- Modify: `scripts/test-production-ops.sh`

- [ ] **Step 1: Write failing deploy tests**

Extend the fake Docker to emit five healthy rows for `compose ps --format`; add fake `git` and `curl` binaries. Run `bash scripts/deploy-fast.sh abc123` and assert the log contains `git pull --ff-only`, `pull backend frontend`, `up -d --no-build --remove-orphans`, a health-format query, and a curl request to `/api/health`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `bash scripts/test-production-ops.sh`

Expected: nonzero exit because deploy does not wait for health or call the health URL.

- [ ] **Step 3: Implement scripts**

Each script sources the helper, loads .env, and uses `production_compose`. Deploy and rollback must poll `production_compose ps --format '{{.Name}} {{.Health}}'` for 20 attempts at 3-second intervals; timeout prints unhealthy rows plus `production_compose ps` and exits 1. Success calls:

```bash
curl -fsS --retry 10 --retry-delay 3 "http://127.0.0.1:$FRONTEND_PORT/api/health"
```

with `FRONTEND_PORT` defaulted to 80. Preserve existing restore validation and MySQL dump flags.

- [ ] **Step 4: Run tests to verify they pass**

Run: `bash scripts/test-production-ops.sh`

Expected: exit 0; successful deploy logs no-build startup, Compose health, and HTTP health.

- [ ] **Step 5: Commit**

```bash
git add scripts/lib scripts/test-production-ops.sh scripts/backup-mysql.sh scripts/deploy-fast.sh scripts/restore-mysql.sh scripts/rollback.sh
git commit -m "feat: verify production deployment health"
```

### Task 3: Deploy exact Git SHA from GitHub Actions

**Files:**
- Modify: `.github/workflows/cd.yml`
- Modify: `scripts/test-production-ops.sh`

- [ ] **Step 1: Write failing workflow contract**

Add assertions that CD includes an `IMAGE_TAG` value equal to the GitHub commit SHA expression, retains SHA tags for both images, and contains the health curl command.

- [ ] **Step 2: Run it to verify failure**

Run: `bash scripts/test-production-ops.sh`

Expected: nonzero exit because the deploy job presently uses `latest`.

- [ ] **Step 3: Implement immutable deploy**

Change only the deploy job’s image-tag environment to the current GitHub commit SHA. Keep both build tags: SHA and latest. After remote no-build startup, add Task 2’s bounded health loop. On timeout, print Compose status and the last 100 log lines for backend, frontend, mysql, redis, and rabbitmq, then exit 1. On success, request the local frontend `/api/health` endpoint with ten curl retries.

- [ ] **Step 4: Run contract and Compose validation**

Run: `bash scripts/test-production-ops.sh && IMAGE_REGISTRY=ghcr.io/example/ruru IMAGE_TAG=abc123 docker compose -f docker-compose.yml -f docker-compose.prod.yml config`

Expected: shell contract passes and rendered backend/frontend images end with `:abc123`.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/cd.yml scripts/test-production-ops.sh
git commit -m "ci: deploy immutable GHCR image tags"
```

### Task 4: Unify production documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/deploy.md`
- Modify: `docs/operations.md`
- Modify: `docs/cd.md`
- Modify: `.env.example`
- Modify: `scripts/test-production-ops.sh`

- [ ] **Step 1: Write failing document contracts**

Assert deployment docs contain `bash scripts/deploy-fast.sh <git-sha>`; operations contain backup, destructive restore, and `bash scripts/rollback.sh <previous-git-sha>`; and deploy docs do not describe server-side `docker compose up -d --build` as the normal update command.

- [ ] **Step 2: Run the document contract to verify failure**

Run: `bash scripts/test-production-ops.sh`

Expected: nonzero exit because `docs/deploy.md` still describes server-side builds as standard updates.

- [ ] **Step 3: Write unified instructions**

Document this production update:

```bash
cd /home/violet/study-flow
git pull --ff-only
bash scripts/deploy-fast.sh <git-sha>
```

Document health, logs, backup, backup-first destructive restore, and SHA rollback in operations. Document the seven server-deploy secrets in `docs/cd.md`. Keep `IMAGE_TAG=latest` in .env.example only as an interactive fallback; explain that CD supplies a SHA.

- [ ] **Step 4: Run all checks**

Run: `bash scripts/test-production-ops.sh && cd backend && mvn -B test && cd ../frontend && npm.cmd run build && npm.cmd run lint`

Expected: shell contracts pass; backend has zero failures/errors; frontend build and lint exit 0.

- [ ] **Step 5: Commit**

```bash
git add README.md docs/deploy.md docs/operations.md docs/cd.md .env.example scripts/test-production-ops.sh
git commit -m "docs: document production deployment recovery"
```

### Task 5: Final release verification and push

**Files:**
- Verify: all Task 1–4 files

- [ ] **Step 1: Verify intended scope**

Run: `git status --short && git diff --check`

Expected: no whitespace errors and unrelated `frontend/src/api/auth.ts` remains unstaged.

- [ ] **Step 2: Re-run full verification**

Run: `bash scripts/test-production-ops.sh && cd backend && mvn -B test && cd ../frontend && npm.cmd run build && npm.cmd run lint`

Expected: script tests pass, Maven reports zero failures/errors, and both frontend commands exit 0.

- [ ] **Step 3: Push Phase 1 commits**

```bash
git push origin main
```

Expected: Actions runs CI/CD; its image build emits latest and SHA tags, while the server deploy uses the SHA tag when deployment secrets are present.

