#!/usr/bin/env bash
set -euo pipefail

MODE="rebuild"
DB_NAME="${DB_NAME:-overseas_purchase}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-overseas-mysql}"
BACKEND_CONTAINER="${BACKEND_CONTAINER:-overseas-backend}"
BACKEND_LOG_PATH="${BACKEND_LOG_PATH:-/root/overseas-purchase/deploy/backend/backend.log}"
SHOW_LOGS=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SCHEMA_FILE="${PROJECT_ROOT}/src/main/resources/db/schema.sql"
DATA_FILE="${PROJECT_ROOT}/src/main/resources/db/data.sql"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/mysql_rebuild_and_verify.sh [options]

Options:
  --mode rebuild|encoding-only   Default: rebuild
  --db-name NAME                 Default: overseas_purchase
  --mysql-container NAME         Default: overseas-mysql
  --backend-container NAME       Default: overseas-backend
  --backend-log-path PATH        Default: /root/overseas-purchase/deploy/backend/backend.log
  --no-logs                      Skip backend log collection
  -h, --help                     Show this help

Environment variables:
  DB_NAME, MYSQL_CONTAINER, BACKEND_CONTAINER, BACKEND_LOG_PATH
  MYSQL_USER, MYSQL_PASSWORD, MYSQL_HOST, MYSQL_PORT
  MYSQL_ROOT_PASSWORD, DB_PASSWORD
  MYSQL_DOCKER_USER, MYSQL_DOCKER_PASSWORD
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODE="${2:-}"
      shift 2
      ;;
    --db-name)
      DB_NAME="${2:-}"
      shift 2
      ;;
    --mysql-container)
      MYSQL_CONTAINER="${2:-}"
      shift 2
      ;;
    --backend-container)
      BACKEND_CONTAINER="${2:-}"
      shift 2
      ;;
    --backend-log-path)
      BACKEND_LOG_PATH="${2:-}"
      shift 2
      ;;
    --no-logs)
      SHOW_LOGS=0
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      usage
      exit 1
      ;;
  esac
done

if [[ "${MODE}" != "rebuild" && "${MODE}" != "encoding-only" ]]; then
  echo "Invalid --mode value: ${MODE}"
  exit 1
fi

if [[ ! -f "${SCHEMA_FILE}" ]]; then
  echo "schema.sql not found: ${SCHEMA_FILE}"
  exit 1
fi

if [[ "${MODE}" == "rebuild" && ! -f "${DATA_FILE}" ]]; then
  echo "data.sql not found: ${DATA_FILE}"
  exit 1
fi

use_docker_mysql=0
if command -v docker >/dev/null 2>&1; then
  if docker ps -a --format '{{.Names}}' | grep -Fxq "${MYSQL_CONTAINER}"; then
    use_docker_mysql=1
  fi
fi

MYSQL_BASE_CMD=()
if [[ ${use_docker_mysql} -eq 1 ]]; then
  is_running="$(docker inspect -f '{{.State.Running}}' "${MYSQL_CONTAINER}" 2>/dev/null || true)"
  if [[ "${is_running}" != "true" ]]; then
    echo "MySQL container exists but is not running: ${MYSQL_CONTAINER}"
    exit 1
  fi

  MYSQL_DOCKER_USER="${MYSQL_DOCKER_USER:-root}"
  MYSQL_DOCKER_PASSWORD="${MYSQL_DOCKER_PASSWORD:-${MYSQL_ROOT_PASSWORD:-${MYSQL_PASSWORD:-${DB_PASSWORD:-}}}}"
  if [[ -z "${MYSQL_DOCKER_PASSWORD}" ]]; then
    echo "Missing MySQL password for Docker mode. Set MYSQL_DOCKER_PASSWORD or MYSQL_ROOT_PASSWORD."
    exit 1
  fi

  MYSQL_BASE_CMD=(
    docker exec -i "${MYSQL_CONTAINER}"
    mysql --default-character-set=utf8mb4
    "-u${MYSQL_DOCKER_USER}" "-p${MYSQL_DOCKER_PASSWORD}"
  )
  echo "Detected MySQL source: docker container (${MYSQL_CONTAINER})"
else
  if ! command -v mysql >/dev/null 2>&1; then
    echo "mysql client not found and docker container not detected."
    exit 1
  fi

  MYSQL_USER="${MYSQL_USER:-root}"
  MYSQL_PASSWORD="${MYSQL_PASSWORD:-${DB_PASSWORD:-${MYSQL_ROOT_PASSWORD:-}}}"
  MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
  MYSQL_PORT="${MYSQL_PORT:-3306}"

  MYSQL_BASE_CMD=(
    mysql --default-character-set=utf8mb4
    "-h${MYSQL_HOST}" "-P${MYSQL_PORT}" "-u${MYSQL_USER}"
  )
  if [[ -n "${MYSQL_PASSWORD}" ]]; then
    MYSQL_BASE_CMD+=("-p${MYSQL_PASSWORD}")
  fi
  echo "Detected MySQL source: host mysql client (${MYSQL_HOST}:${MYSQL_PORT})"
fi

mysql_stdin() {
  local db="${1:-}"
  if [[ -n "${db}" ]]; then
    "${MYSQL_BASE_CMD[@]}" "${db}"
  else
    "${MYSQL_BASE_CMD[@]}"
  fi
}

mysql_print() {
  local query="$1"
  local db="${2:-}"
  if [[ -n "${db}" ]]; then
    "${MYSQL_BASE_CMD[@]}" -e "${query}" "${db}"
  else
    "${MYSQL_BASE_CMD[@]}" -e "${query}"
  fi
}

mysql_query_no_header() {
  local query="$1"
  local db="${2:-}"
  if [[ -n "${db}" ]]; then
    "${MYSQL_BASE_CMD[@]}" --batch --skip-column-names -e "${query}" "${db}"
  else
    "${MYSQL_BASE_CMD[@]}" --batch --skip-column-names -e "${query}"
  fi
}

echo "Preparing database ${DB_NAME} with utf8mb4..."
cat <<SQL | mysql_stdin
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER DATABASE \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL

if [[ "${MODE}" == "rebuild" ]]; then
  echo "Importing schema.sql..."
  mysql_stdin "${DB_NAME}" < "${SCHEMA_FILE}"

  echo "Importing data.sql..."
  mysql_stdin "${DB_NAME}" < "${DATA_FILE}"
else
  echo "Applying utf8mb4 conversion on all tables in ${DB_NAME}..."
  table_list="$(mysql_query_no_header "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${DB_NAME}' AND TABLE_TYPE='BASE TABLE';")"
  if [[ -n "${table_list}" ]]; then
    while IFS= read -r table_name; do
      table_name="${table_name//$'\r'/}"
      [[ -z "${table_name}" ]] && continue
      cat <<SQL | mysql_stdin "${DB_NAME}"
ALTER TABLE \`${table_name}\` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL
    done <<< "${table_list}"
  fi
fi

echo
echo "[Verify] character_set variables"
mysql_print "SHOW VARIABLES LIKE 'character_set_%';"
echo
echo "[Verify] collation variables"
mysql_print "SHOW VARIABLES LIKE 'collation_%';"
echo
echo "[Verify] category preview"
mysql_print "SELECT id, name FROM category ORDER BY id LIMIT 20;" "${DB_NAME}"
echo
echo "[Verify] product count"
mysql_print "SELECT COUNT(*) AS product_count FROM product;" "${DB_NAME}"

if [[ ${SHOW_LOGS} -eq 1 ]]; then
  echo
  echo "[Logs] backend latest 200 lines"
  if command -v docker >/dev/null 2>&1 && docker ps -a --format '{{.Names}}' | grep -Fxq "${BACKEND_CONTAINER}"; then
    docker logs "${BACKEND_CONTAINER}" --tail=200 || true
  elif [[ -f "${BACKEND_LOG_PATH}" ]]; then
    tail -n 200 "${BACKEND_LOG_PATH}" || true
  else
    echo "Skip: backend log source not found."
    echo "Hint: set BACKEND_LOG_PATH or BACKEND_CONTAINER."
  fi
fi

echo
echo "Completed. Mode=${MODE}, DB=${DB_NAME}"
