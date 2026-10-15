#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" != "mysqld" ]]; then
  exec /usr/local/bin/docker-entrypoint.sh "$@"
fi

: "${SMARTPG_APP_USER:?SMARTPG_APP_USER is required}"
: "${SMARTPG_APP_PASSWORD:?SMARTPG_APP_PASSWORD is required}"
: "${MYSQLDATABASE:?MYSQLDATABASE is required}"

sql_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\'/\'\'}"
  printf '%s' "$value"
}

app_user="$(sql_escape "$SMARTPG_APP_USER")"
app_password="$(sql_escape "$SMARTPG_APP_PASSWORD")"
database_name="${MYSQLDATABASE//`/\`}"

sql_file="$(mktemp /dev/shm/railway-mysql-init.XXXXXX)"
chmod 0600 "$sql_file"
{
  printf "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED WITH caching_sha2_password BY '%s';\n" "$app_user" "$app_password"
  printf "ALTER USER '%s'@'%%' IDENTIFIED WITH caching_sha2_password BY '%s';\n" "$app_user" "$app_password"
  printf 'GRANT ALL PRIVILEGES ON `%s`.* TO '\''%s'\''@'\''%%'\'';\n' "$database_name" "$app_user"
  printf 'FLUSH PRIVILEGES;\n'
} > "$sql_file"

exec /usr/local/bin/docker-entrypoint.sh mysqld --init-file="$sql_file"
