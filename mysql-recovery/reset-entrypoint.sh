#!/usr/bin/env bash
set -euo pipefail

if [[ "${1:-}" != "mysqld" ]]; then
  exec /usr/local/bin/docker-entrypoint.sh "$@"
fi

: "${SMARTPG_APP_USER:?SMARTPG_APP_USER is required}"
: "${SMARTPG_APP_PASSWORD:?SMARTPG_APP_PASSWORD is required}"
: "${MYSQLDATABASE:?MYSQLDATABASE is required}"

# Keep interpolated SQL values restricted to safe characters.
[[ "$SMARTPG_APP_USER" =~ ^[A-Za-z0-9_]+$ ]] || { echo "Invalid SMARTPG_APP_USER"; exit 1; }
[[ "$SMARTPG_APP_PASSWORD" =~ ^[A-Za-z0-9]+$ ]] || { echo "Invalid SMARTPG_APP_PASSWORD"; exit 1; }
[[ "$MYSQLDATABASE" =~ ^[A-Za-z0-9_]+$ ]] || { echo "Invalid MYSQLDATABASE"; exit 1; }

sql_file="$(mktemp /dev/shm/railway-mysql-init.XXXXXX)"
chmod 0600 "$sql_file"
{
  printf "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED WITH caching_sha2_password BY '%s';\n" "$SMARTPG_APP_USER" "$SMARTPG_APP_PASSWORD"
  printf "ALTER USER '%s'@'%%' IDENTIFIED WITH caching_sha2_password BY '%s';\n" "$SMARTPG_APP_USER" "$SMARTPG_APP_PASSWORD"
  printf "GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'%%';\n" "$MYSQLDATABASE" "$SMARTPG_APP_USER"
  printf "FLUSH PRIVILEGES;\n"
} > "$sql_file"

exec /usr/local/bin/docker-entrypoint.sh mysqld --init-file="$sql_file"
