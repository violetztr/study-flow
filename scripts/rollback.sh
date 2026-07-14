#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: bash scripts/rollback.sh <previous-git-sha>" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=./lib/production-compose.sh
source "$SCRIPT_DIR/lib/production-compose.sh"

load_production_env

IMAGE_TAG="$1"
export IMAGE_TAG

FRONTEND_PORT="${FRONTEND_PORT:-80}"
HEALTH_RETRIES=20
HEALTH_DELAY=3

echo "Rolling Ruru back to image tag: $IMAGE_TAG"

echo "Pulling images for tag $IMAGE_TAG..."
production_compose pull backend frontend

echo "Starting services (no-build)..."
production_compose up -d --no-build --remove-orphans

echo "Waiting for services to become healthy..."
for i in $(seq 1 "$HEALTH_RETRIES"); do
  unhealthy=$(production_compose ps --format '{{.Name}} {{.Health}}' 2>/dev/null | grep -v ' healthy' || true)
  if [ -z "$unhealthy" ]; then
    echo "All services healthy after $i attempt(s)."
    break
  fi
  if [ "$i" -eq "$HEALTH_RETRIES" ]; then
    echo "ERROR: Some services are still unhealthy after ${HEALTH_RETRIES} attempts:" >&2
    echo "$unhealthy" >&2
    echo "" >&2
    echo "Full compose status:" >&2
    production_compose ps >&2
    exit 1
  fi
  sleep "$HEALTH_DELAY"
done

echo "Verifying frontend-proxied health endpoint..."
curl -fsS --retry 10 --retry-delay 3 "http://127.0.0.1:${FRONTEND_PORT}/api/health"

echo "Rollback complete."
production_compose ps
