#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=./lib/production-compose.sh
source "$SCRIPT_DIR/lib/production-compose.sh"

load_production_env

DB_NAME="${MYSQL_DATABASE:-study_flow}"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups/mysql}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/${DB_NAME}-${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

production_compose exec -T mysql sh -c \
  'mysqldump --single-transaction --quick --routines --triggers -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  | gzip -9 > "$BACKUP_FILE"

echo "MySQL backup created: $BACKUP_FILE"
