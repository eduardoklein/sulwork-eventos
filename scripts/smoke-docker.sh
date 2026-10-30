#!/usr/bin/env bash
# Teste de fumaça do sistema no Docker Compose, pelo mesmo endereço que o navegador usa (nginx na frente).
#
#   docker compose up --build -d
#   scripts/smoke-docker.sh                       # usa http://localhost:3000
#   scripts/smoke-docker.sh http://localhost:3002
#
# Confere duas coisas que os testes de integração não alcançam:
#   1. Uma escrita (POST) feita como o navegador faz, com o cabeçalho Origin, passa pelo proxy e pelo CORS.
#   2. 10 pedidos ao mesmo tempo para um item de 1 unidade dão 1 resposta 201 e 9 respostas 422.
# O script cria um evento de teste e o cancela no fim. O evento cancelado continua na lista.
set -u

BASE_URL="${1:-http://localhost:3000}"
BASE_URL="${BASE_URL%/}"
API="$BASE_URL/api"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FAILED=0

pass() { echo "  ok   $1"; }
fail() { echo "  FALHA $1"; FAILED=1; }

# Chama a API como o navegador: com o cabeçalho Origin da própria página. Grava o corpo em $WORK/body e imprime o código.
call() {
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    curl -s -o "$WORK/body" -w '%{http_code}' -X "$method" "$API$path" -H "Origin: $BASE_URL" -H 'Content-Type: application/json' -d "$body"
  else
    curl -s -o "$WORK/body" -w '%{http_code}' -X "$method" "$API$path" -H "Origin: $BASE_URL"
  fi
}

id_of_body() { grep -o '"id":[0-9]*' "$WORK/body" | head -1 | cut -d: -f2; }

# CPF válido a partir de 9 dígitos: acrescenta os dois dígitos verificadores.
cpf_from() {
  local base="$1" d1 d2 sum i
  sum=0; for i in $(seq 0 8); do sum=$((sum + ${base:$i:1} * (10 - i))); done
  d1=$(( (sum * 10) % 11 )); [ "$d1" -eq 10 ] && d1=0
  sum=0; for i in $(seq 0 8); do sum=$((sum + ${base:$i:1} * (11 - i))); done
  sum=$((sum + d1 * 2))
  d2=$(( (sum * 10) % 11 )); [ "$d2" -eq 10 ] && d2=0
  echo "${base}${d1}${d2}"
}

echo "Teste de fumaça em $BASE_URL"

echo "1. Escrita pelo proxy, com o cabeçalho Origin do navegador"
code=$(call POST /events '{"name":"Teste de fumaça","type":"GET_TOGETHER","dateTime":"2099-12-31T20:00:00","changeDeadline":"2099-12-30T18:00:00"}')
if [ "$code" = "201" ]; then pass "POST /api/events respondeu 201"; else fail "POST /api/events respondeu $code (esperado 201). O corpo: $(cat "$WORK/body")"; fi
EVENT_ID="$(id_of_body)"
if [ -z "$EVENT_ID" ]; then echo "Sem o evento de teste, não dá para seguir."; exit 1; fi

code=$(call POST "/events/$EVENT_ID/categories" '{"name":"Pratos","unit":"UNIT","goalQuantity":0}')
[ "$code" = "201" ] && pass "categoria criada" || fail "categoria respondeu $code"
CATEGORY_ID="$(id_of_body)"

code=$(call POST "/events/$EVENT_ID/items" "{\"name\":\"Item de uma unidade\",\"categoryId\":$CATEGORY_ID,\"requiredQuantity\":1}")
[ "$code" = "201" ] && pass "item de 1 unidade criado" || fail "item respondeu $code"
ITEM_ID="$(id_of_body)"

code=$(call PATCH "/events/$EVENT_ID/status" '{"newStatus":"ABERTO"}')
[ "$code" = "200" ] && pass "evento aberto" || fail "abrir o evento respondeu $code"

echo "2. Inscrição de 10 participantes"
PARTICIPANTS=()
for n in $(seq 1 10); do
  cpf="$(cpf_from "$(printf '9%08d' $((RANDOM * 100 + n)))")"
  code=$(call POST "/events/$EVENT_ID/participants" "{\"cpf\":\"$cpf\",\"name\":\"Pessoa $n\",\"restrictions\":[]}")
  if [ "$code" = "201" ]; then PARTICIPANTS+=("$(id_of_body)"); else fail "inscrever a pessoa $n respondeu $code"; fi
done
[ "${#PARTICIPANTS[@]}" -eq 10 ] && pass "10 participantes inscritos" || { echo "Faltam participantes, não dá para seguir."; exit 1; }

echo "3. Concorrência: 10 pedidos ao mesmo tempo para um item de 1 unidade"
for n in $(seq 0 9); do
  (
    curl -s -o /dev/null -w '%{http_code}\n' -X POST "$API/events/$EVENT_ID/contributions" \
      -H "Origin: $BASE_URL" -H 'Content-Type: application/json' \
      -d "{\"participantId\":${PARTICIPANTS[$n]},\"itemId\":$ITEM_ID,\"quantity\":1}" > "$WORK/result-$n"
  ) &
done
wait
CREATED=$(cat "$WORK"/result-* | grep -c '^201$')
REFUSED=$(cat "$WORK"/result-* | grep -c '^422$')
OTHERS=$(cat "$WORK"/result-* | grep -vc -e '^201$' -e '^422$')
echo "     201: $CREATED | 422: $REFUSED | outras: $OTHERS"
if [ "$CREATED" -eq 1 ] && [ "$REFUSED" -eq 9 ]; then pass "1 resposta 201 e 9 respostas 422"; else fail "esperado 1 resposta 201 e 9 respostas 422"; fi

code=$(call GET "/events/$EVENT_ID/items")
TOTAL=$(grep -o '"contributedQuantity":[0-9]*' "$WORK/body" | head -1 | cut -d: -f2)
[ "$TOTAL" = "1" ] && pass "o item tem 1 unidade contribuída, e nunca passa do necessário" || fail "contribuído: ${TOTAL:-?} (esperado 1)"

echo "4. Limpeza"
code=$(call PATCH "/events/$EVENT_ID/status" '{"newStatus":"CANCELADO"}')
[ "$code" = "200" ] && pass "evento de teste cancelado" || fail "cancelar respondeu $code"

if [ "$FAILED" -eq 0 ]; then echo "Tudo certo."; else echo "Há falhas."; fi
exit "$FAILED"
