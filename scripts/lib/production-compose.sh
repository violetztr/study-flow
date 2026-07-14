#!/usr/bin/env bash

# Shared production Compose helper for Ruru.
# Sources this file in operational scripts to use consistent Compose file selection,
# environment loading, and Docker/sudo detection.

set -euo pipefail

production_root_dir() {
  (cd "$(dirname "${BASH_SOURCE[1]:-${BASH_SOURCE[0]}}")/../.." && pwd)
}

load_production_env() {
  local root_dir
  root_dir="$(production_root_dir)"

  if [ -f "$root_dir/.env" ]; then
    set -a
    # shellcheck disable=SC1091
    . "$root_dir/.env"
    set +a
  fi
}

production_compose() {
  local root_dir
  root_dir="$(production_root_dir)"

  local image_registry="${IMAGE_REGISTRY:-ghcr.io/violetztr/study-flow}"
  local image_tag="${IMAGE_TAG:-latest}"

  if docker ps >/dev/null 2>&1; then
    IMAGE_REGISTRY="$image_registry" IMAGE_TAG="$image_tag" \
      docker compose -f "$root_dir/docker-compose.yml" -f "$root_dir/docker-compose.prod.yml" "$@"
  else
    sudo env IMAGE_REGISTRY="$image_registry" IMAGE_TAG="$image_tag" \
      docker compose -f "$root_dir/docker-compose.yml" -f "$root_dir/docker-compose.prod.yml" "$@"
  fi
}
