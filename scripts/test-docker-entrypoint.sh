#!/bin/sh
# Testa a conversão de DATABASE_URL (Render) para as variáveis do Spring, sem Docker e sem banco.
#   scripts/test-docker-entrypoint.sh
ENTRYPOINT="$(dirname "$0")/../backend/docker-entrypoint.sh"
FAILED=0

# check "nome do caso" "saída esperada" VAR=valor ...
check() {
  name="$1"; expected="$2"; shift 2
  actual="$(env -i PATH="$PATH" ENTRYPOINT_DRY_RUN=1 "$@" sh "$ENTRYPOINT")"
  if [ "$actual" = "$expected" ]; then
    echo "  ok   $name"
  else
    echo "  FALHA $name"; echo "    esperado: $(echo "$expected" | tr '\n' ' ')"; echo "    recebido: $(echo "$actual" | tr '\n' ' ')"
    FAILED=1
  fi
}

echo "Entrypoint do backend"

check "URL do Render, sem porta (usa 5432)" "DB_URL=jdbc:postgresql://dpg-abc123-a:5432/sulwork_db
DB_USERNAME=sulwork_user
DB_PASSWORD=s3cr3t" DATABASE_URL="postgresql://sulwork_user:s3cr3t@dpg-abc123-a/sulwork_db"

check "URL com porta" "DB_URL=jdbc:postgresql://db.example.com:6543/app
DB_USERNAME=u
DB_PASSWORD=p" DATABASE_URL="postgres://u:p@db.example.com:6543/app"

check "URL com parâmetros no fim" "DB_URL=jdbc:postgresql://host:5432/app
DB_USERNAME=u
DB_PASSWORD=p" DATABASE_URL="postgres://u:p@host:5432/app?sslmode=require"

check "senha com dois-pontos" "DB_URL=jdbc:postgresql://host:5432/app
DB_USERNAME=u
DB_PASSWORD=a:b:c" DATABASE_URL="postgres://u:a:b:c@host:5432/app"

check "DB_URL já definida vence (Docker Compose)" "DB_URL=jdbc:postgresql://db:5432/sulwork
DB_USERNAME=sulwork
DB_PASSWORD=sulwork" DB_URL="jdbc:postgresql://db:5432/sulwork" DB_USERNAME=sulwork DB_PASSWORD=sulwork DATABASE_URL="postgres://outro:outro@outro/outro"

check "sem DATABASE_URL, nada muda" "DB_URL=
DB_USERNAME=
DB_PASSWORD=" X=1

if [ "$FAILED" -eq 0 ]; then echo "Tudo certo."; else echo "Há falhas."; fi
exit "$FAILED"
