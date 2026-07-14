#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: bash scripts/restore-mysql.sh /path/to/backup.sql.gz" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKUP_FILE="$1"

cd "$ROOT_DIR"

# shellcheck source=./lib/production-compose.sh
source "$SCRIPT_DIR/lib/production-compose.sh"

load_production_env

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Backup file not found: $BACKUP_FILE" >&2
  exit 1
fi

echo "Restoring MySQL from: $BACKUP_FILE"
echo "This will write into database: ${MYSQL_DATABASE:-study_flow}"
echo "WARNING: This is a destructive operation. Make sure you have a recent backup before proceeding."
gunzip -c "$BACKUP_FILE" | production_compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'

echo "MySQL restore completed."
