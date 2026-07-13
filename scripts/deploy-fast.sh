#!/usr/bin/env bash
set -euo pipefail

# Fast production deploy: pull prebuilt GHCR images, then restart without building on the server.
git pull --ff-only
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml pull backend frontend
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --no-build --remove-orphans
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
