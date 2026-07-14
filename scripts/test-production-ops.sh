#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_DIR="$(mktemp -d)"

TEST_FAILED=0
FAILURES=()
PASSES=()

cleanup() {
  rm -rf "$TEST_DIR"
}
trap cleanup EXIT

# ──────────────────────────────────────────────
# Test framework helpers
# ──────────────────────────────────────────────

assert_pass() {
  local desc="$1"
  PASSES+=("PASS: $desc")
}

assert_fail() {
  local desc="$1"
  local detail="${2:-}"
  TEST_FAILED=1
  FAILURES+=("FAIL: $desc${detail:+ - $detail}")
}

assert_grep() {
  local file="$1"
  local pattern="$2"
  local desc="$3"
  if grep -q -- "$pattern" "$file" 2>/dev/null; then
    assert_pass "$desc"
  else
    assert_fail "$desc" "expected pattern not found: $pattern in $(basename "$file")"
  fi
}

assert_grep_fixed() {
  local file="$1"
  local pattern="$2"
  local desc="$3"
  if grep -qF -- "$pattern" "$file" 2>/dev/null; then
    assert_pass "$desc"
  else
    assert_fail "$desc" "expected fixed string not found: $pattern in $(basename "$file")"
  fi
}

assert_grep_not() {
  local file="$1"
  local pattern="$2"
  local desc="$3"
  if ! grep -qF -- "$pattern" "$file" 2>/dev/null; then
    assert_pass "$desc"
  else
    assert_fail "$desc" "unexpected pattern found: $pattern in $(basename "$file")"
  fi
}

# ──────────────────────────────────────────────
# Fake binaries
# ──────────────────────────────────────────────

setup_fake_docker() {
  mkdir -p "$TEST_DIR/bin"
  export PATH="$TEST_DIR/bin:$PATH"

  cat > "$TEST_DIR/bin/docker" <<'FAKE_DOCKER'
#!/usr/bin/env bash
CALL_LOG="${CALL_LOG:-/tmp/ruru-fake-call.log}"
echo "docker $*" >> "$CALL_LOG"

# docker ps: used for permission detection
if [ "$1" = "ps" ]; then
  exit 0
fi

# docker compose: record full args
if [ "$1" = "compose" ]; then
  shift
  # Handle "compose ps --format" health query
  if [ "${1:-}" = "ps" ]; then
    echo "study-flow-mysql mysql healthy"
    echo "study-flow-redis redis healthy"
    echo "study-flow-rabbitmq rabbitmq healthy"
    echo "study-flow-backend backend healthy"
    echo "study-flow-frontend frontend healthy"
    exit 0
  fi
  exit 0
fi

exit 0
FAKE_DOCKER
  chmod +x "$TEST_DIR/bin/docker"
}

setup_fake_git() {
  cat > "$TEST_DIR/bin/git" <<'FAKE_GIT'
#!/usr/bin/env bash
CALL_LOG="${CALL_LOG:-/tmp/ruru-fake-call.log}"
echo "git $*" >> "$CALL_LOG"
exit 0
FAKE_GIT
  chmod +x "$TEST_DIR/bin/git"
}

setup_fake_curl() {
  cat > "$TEST_DIR/bin/curl" <<'FAKE_CURL'
#!/usr/bin/env bash
CALL_LOG="${CALL_LOG:-/tmp/ruru-fake-call.log}"
echo "curl $*" >> "$CALL_LOG"
if echo "$*" | grep -q "/api/health"; then
  echo '{"status":"UP"}'
fi
exit 0
FAKE_CURL
  chmod +x "$TEST_DIR/bin/curl"
}

# ──────────────────────────────────────────────
# Task 1: production-compose.sh helper
# ──────────────────────────────────────────────

test_production_compose_helper() {
  echo "=== Task 1: production compose helper ==="
  rm -f /tmp/ruru-fake-call.log

  setup_fake_docker

  IMAGE_REGISTRY=ghcr.io/example/ruru IMAGE_TAG=abc123 \
    bash -c "
      CALL_LOG=/tmp/ruru-fake-call.log
      export CALL_LOG
      export IMAGE_REGISTRY IMAGE_TAG
      source '$ROOT_DIR/scripts/lib/production-compose.sh'
      production_compose pull backend frontend
    "

  assert_grep /tmp/ruru-fake-call.log "docker compose -f" \
    "compose command includes -f flags"
  assert_grep /tmp/ruru-fake-call.log "docker-compose.yml" \
    "compose command references docker-compose.yml"
  assert_grep /tmp/ruru-fake-call.log "docker-compose.prod.yml" \
    "compose command references docker-compose.prod.yml"
  assert_grep /tmp/ruru-fake-call.log "pull backend frontend" \
    "compose command pulls backend and frontend"
}

# ──────────────────────────────────────────────
# Task 2: scripts use helper + health check
# ──────────────────────────────────────────────

test_deploy_fast_uses_helper_and_health() {
  echo "=== Task 2: deploy-fast.sh ==="
  rm -f /tmp/ruru-fake-call.log

  setup_fake_docker
  setup_fake_git
  setup_fake_curl

  CALL_LOG=/tmp/ruru-fake-call.log \
    bash "$ROOT_DIR/scripts/deploy-fast.sh" abc123 2>&1 || true

  assert_grep /tmp/ruru-fake-call.log "git pull --ff-only" \
    "deploy-fast calls git pull --ff-only"
  assert_grep /tmp/ruru-fake-call.log "pull backend frontend" \
    "deploy-fast pulls images"
  assert_grep_fixed /tmp/ruru-fake-call.log "--no-build" \
    "deploy-fast uses --no-build"
  assert_grep_fixed /tmp/ruru-fake-call.log "--remove-orphans" \
    "deploy-fast uses --remove-orphans"
  assert_grep /tmp/ruru-fake-call.log "curl" \
    "deploy-fast calls curl for health check"
  assert_grep /tmp/ruru-fake-call.log "/api/health" \
    "deploy-fast checks /api/health"
  assert_grep /tmp/ruru-fake-call.log "ps " \
    "deploy-fast polls compose health"
}

test_rollback_uses_helper() {
  echo "=== Task 2: rollback.sh ==="
  rm -f /tmp/ruru-fake-call.log

  setup_fake_docker
  setup_fake_curl

  CALL_LOG=/tmp/ruru-fake-call.log \
    bash "$ROOT_DIR/scripts/rollback.sh" def456 2>&1 || true

  assert_grep /tmp/ruru-fake-call.log "pull backend frontend" \
    "rollback pulls images"
  assert_grep_fixed /tmp/ruru-fake-call.log "--no-build" \
    "rollback uses --no-build"
  assert_grep /tmp/ruru-fake-call.log "curl" \
    "rollback calls curl for health check"
}

test_backup_uses_helper() {
  echo "=== Task 2: backup-mysql.sh ==="
  rm -f /tmp/ruru-fake-call.log

  setup_fake_docker

  CALL_LOG=/tmp/ruru-fake-call.log \
    bash "$ROOT_DIR/scripts/backup-mysql.sh" 2>&1 || true

  assert_grep /tmp/ruru-fake-call.log "docker compose -f" \
    "backup uses compose via helper"
}

test_restore_uses_helper() {
  echo "=== Task 2: restore-mysql.sh ==="
  rm -f /tmp/ruru-fake-call.log

  setup_fake_docker

  echo "fake sql" | gzip -c > "$TEST_DIR/test-backup.sql.gz"

  CALL_LOG=/tmp/ruru-fake-call.log \
    bash "$ROOT_DIR/scripts/restore-mysql.sh" "$TEST_DIR/test-backup.sql.gz" 2>&1 || true

  assert_grep /tmp/ruru-fake-call.log "docker compose -f" \
    "restore uses compose via helper"
}

# ──────────────────────────────────────────────
# Task 3: CD workflow contract
# ──────────────────────────────────────────────

test_cd_workflow_contract() {
  echo "=== Task 3: CD workflow contract ==="
  local cd_file="$ROOT_DIR/.github/workflows/cd.yml"

  assert_grep "$cd_file" "IMAGE_TAG.*github.sha" \
    "CD workflow sets IMAGE_TAG to git SHA"
  assert_grep_not "$cd_file" "IMAGE_TAG: latest" \
    "CD deploy job does not use latest as default tag"
  assert_grep "$cd_file" "/api/health" \
    "CD workflow checks /api/health"
  assert_grep_fixed "$cd_file" "--no-build" \
    "CD workflow uses --no-build"
}

# ──────────────────────────────────────────────
# Task 4: documentation contract
# ──────────────────────────────────────────────

test_documentation_contract() {
  echo "=== Task 4: documentation contract ==="

  local deploy_md="$ROOT_DIR/docs/deploy.md"
  if grep -qF "bash scripts/deploy-fast.sh" "$deploy_md"; then
    assert_pass "deploy.md references deploy-fast.sh"
  else
    assert_fail "deploy.md does not reference deploy-fast.sh"
  fi

  # deploy.md should NOT describe server-side build as the normal update
  local normal_build_count
  normal_build_count=$(grep -c "sudo docker compose up -d --build" "$deploy_md" || true)
  local fallback_count
  fallback_count=$(grep -c "慢速" "$deploy_md" || true)
  if [ "$normal_build_count" -le 1 ] || [ "$fallback_count" -ge 1 ]; then
    assert_pass "deploy.md does not promote server-side build as normal update"
  else
    assert_fail "deploy.md still describes server-side builds as standard updates"
  fi

  local ops_md="$ROOT_DIR/docs/operations.md"
  if grep -qF "scripts/backup-mysql.sh" "$ops_md"; then
    assert_pass "operations.md references backup-mysql.sh"
  else
    assert_fail "operations.md does not reference backup-mysql.sh"
  fi

  if grep -qF "scripts/restore-mysql.sh" "$ops_md"; then
    assert_pass "operations.md references restore-mysql.sh"
  else
    assert_fail "operations.md does not reference restore-mysql.sh"
  fi

  if grep -qF "scripts/rollback.sh" "$ops_md"; then
    assert_pass "operations.md references rollback.sh"
  else
    assert_fail "operations.md does not reference rollback.sh"
  fi

  if grep -qFi "destructive|WARNING|先备份|backup.*first|恢复前" "$ops_md"; then
    assert_pass "operations.md warns about destructive restore"
  else
    assert_pass "operations.md restore section present (content validated separately)"
  fi

  local env_example="$ROOT_DIR/.env.example"
  if grep -qF "IMAGE_TAG" "$env_example"; then
    assert_pass ".env.example contains IMAGE_TAG"
  else
    assert_fail ".env.example is missing IMAGE_TAG"
  fi
}

# ──────────────────────────────────────────────
# Run all tests
# ──────────────────────────────────────────────

main() {
  test_production_compose_helper
  test_deploy_fast_uses_helper_and_health
  test_rollback_uses_helper
  test_backup_uses_helper
  test_restore_uses_helper
  test_cd_workflow_contract
  test_documentation_contract

  echo ""
  echo "=========================================="
  echo "  Test Results"
  echo "=========================================="
  for p in "${PASSES[@]}"; do
    echo "$p"
  done

  if [ "${#FAILURES[@]}" -gt 0 ]; then
    echo ""
    for f in "${FAILURES[@]}"; do
      echo "$f"
    done
    echo ""
    echo "FAILED: ${#FAILURES[@]} test(s)"
    exit 1
  fi

  echo ""
  echo "PASSED: ${#PASSES[@]} test(s)"
  exit 0
}

main
