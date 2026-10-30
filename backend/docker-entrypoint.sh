#!/bin/sh
# O Render entrega o banco em uma URL só (postgres://usuario:senha@host:porta/banco), em DATABASE_URL.
# O Spring quer a URL JDBC, o usuário e a senha em separado. Se DB_URL já existe (o Docker Compose a define),
# nada muda.
if [ -n "${DATABASE_URL:-}" ] && [ -z "${DB_URL:-}" ]; then
  rest="${DATABASE_URL#*://}"
  credentials="${rest%%@*}"
  location="${rest#*@}"
  hostport="${location%%/*}"
  database="${location#*/}"
  database="${database%%\?*}"
  case "$hostport" in
    *:*) ;;
    *) hostport="$hostport:5432" ;;
  esac
  export DB_URL="jdbc:postgresql://$hostport/$database"
  export DB_USERNAME="${credentials%%:*}"
  export DB_PASSWORD="${credentials#*:}"
fi

# Só para o teste scripts/test-docker-entrypoint.sh: mostra o resultado e sai, sem subir o Java.
if [ -n "${ENTRYPOINT_DRY_RUN:-}" ]; then
  echo "DB_URL=${DB_URL:-}"
  echo "DB_USERNAME=${DB_USERNAME:-}"
  echo "DB_PASSWORD=${DB_PASSWORD:-}"
  exit 0
fi

exec java -jar /app/app.jar
