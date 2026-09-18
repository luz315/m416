#!/usr/bin/env bash
# 빌드된 두 애플리케이션으로 정상·부분 실패·전체 실패·복구를 검증한다.
set -euo pipefail
cd "$(dirname "$0")/.."
for tool in java curl jq; do
  command -v "$tool" >/dev/null || { echo "필요한 도구: $tool" >&2; exit 2; }
done
smoke_mysql_url="${SMOKE_MYSQL_URL:-}"
smoke_mysql_username="${SMOKE_MYSQL_USERNAME:-}"
smoke_mysql_password="${SMOKE_MYSQL_PASSWORD:-}"
if [[ -z "$smoke_mysql_url" || -z "$smoke_mysql_username" ]]; then
  echo "SMOKE_MYSQL_URL 및 SMOKE_MYSQL_USERNAME을 설정한 전용 MySQL DB가 필요합니다." >&2
  exit 2
fi
api_port="${SMOKE_API_PORT:-18080}"
mock_port="${SMOKE_MOCK_PORT:-19090}"
smoke_dir="$(mktemp -d "${TMPDIR:-/tmp}/stay-smoke.XXXXXX")"
api_pid=''
mock_pid=''
cleanup() {
  if [[ -n "$api_pid" ]]; then kill "$api_pid" 2>/dev/null || true; wait "$api_pid" 2>/dev/null || true; fi
  if [[ -n "$mock_pid" ]]; then kill "$mock_pid" 2>/dev/null || true; wait "$mock_pid" 2>/dev/null || true; fi
  echo "검증 로그: $smoke_dir"
}
trap cleanup EXIT
mock_url="http://127.0.0.1:$mock_port"
api_url="http://127.0.0.1:$api_port"
# 기존 서비스의 모드를 실수로 바꾸지 않도록 이미 사용 중인 포트를 거부한다.
for port in "$api_port" "$mock_port"; do
  if (echo > "/dev/tcp/127.0.0.1/$port") 2>/dev/null; then echo "이미 사용 중인 포트: $port" >&2; exit 2; fi
done
java -jar m416-mock-supplier/build/libs/m416-mock-supplier-0.1.0.jar --server.port="$mock_port" --spring.lifecycle.timeout-per-shutdown-phase=1s > "$smoke_dir/mock.log" 2>&1 &
mock_pid=$!
wait_ready() {
  local url="$1" pid="$2"
  for ((i=0;i<100;i++)); do
    kill -0 "$pid" 2>/dev/null || { echo "서버 시작 실패: $smoke_dir" >&2; return 1; }
    if curl -fsS -H 'X-Api-Key: local-demo-key' --max-time 1 "$url" >/dev/null 2>&1; then return 0; fi
    sleep 0.2
  done
  echo "서버 준비 시간 초과" >&2; return 1
}
wait_ready "$mock_url/a/v1/hotels" "$mock_pid" || {
  # Mock이 준비되지 않으면 검색 서버를 시작하지 않는다.
  cat "$smoke_dir/mock.log" >&2; exit 1;
}
start_api() {
  java -jar m416-rest/build/libs/m416-rest-0.1.0.jar --server.port="$api_port" --spring.lifecycle.timeout-per-shutdown-phase=1s \
    "--spring.datasource.url=$smoke_mysql_url" \
    "--spring.datasource.username=$smoke_mysql_username" \
    "--spring.datasource.password=$smoke_mysql_password" \
    --suppliers.a.base-url="$mock_url" --suppliers.b.base-url="$mock_url" > "$smoke_dir/api.log" 2>&1 &
  api_pid=$!
  wait_ready "$api_url/actuator/health/readiness" "$api_pid"
}
start_api
curl -fsS --max-time 5 "$api_url/v3/api-docs" > "$smoke_dir/openapi.json"
jq -e '.info.title=="Stay Gateway API" and .paths["/api/v1/stays/search"].get != null' "$smoke_dir/openapi.json" >/dev/null
curl -fsSL --max-time 5 "$api_url/swagger-ui.html" | grep -q 'Swagger UI'
search="$api_url/api/v1/stays/search?checkIn=2026-09-01&checkOut=2026-09-04&adults=2&children=0"
curl -fsS --max-time 12 "$search" > "$smoke_dir/normal.json"
jq -e '.status=="SUCCESS" and (.offers|length)==3 and ([.offers[]|select(.supplier=="A" and .price.totalIncludingTax==429000 and .availableRooms==1)]|length)==1 and ([.offers[]|select(.supplier=="B" and .price.totalIncludingTax==452000 and .breakfastIncluded)]|length)==1 and ([.offers[]|select(.availableRooms==0)]|length)==1' "$smoke_dir/normal.json" >/dev/null
curl -fsS -X POST "$mock_url/control/a/mode?value=no-response" >/dev/null
curl -fsS --max-time 12 "$search" > "$smoke_dir/partial.json"
jq -e '.status=="PARTIAL_FAILURE" and (.offers|length)==1 and .offers[0].supplier=="B" and .failures[0].code=="TIMEOUT"' "$smoke_dir/partial.json" >/dev/null
curl -fsS -X POST "$mock_url/control/b/mode?value=error" >/dev/null
curl -fsS --max-time 12 "$search" > "$smoke_dir/failed.json"
jq -e '.status=="ALL_FAILED" and (.offers|length)==0 and ([.failures[].code]|index("UNAVAILABLE"))!=null' "$smoke_dir/failed.json" >/dev/null
curl -fsS -X POST "$mock_url/control/a/mode?value=normal" >/dev/null
curl -fsS -X POST "$mock_url/control/b/mode?value=normal" >/dev/null
code="$(curl -sS -o "$smoke_dir/invalid.json" -w '%{http_code}' "$api_url/api/v1/stays/search?checkIn=2026-09-04&checkOut=2026-09-01&adults=2&children=0")"
[[ "$code" == 400 ]]
kill "$api_pid"; wait "$api_pid" || true; api_pid=''
start_api
curl -fsS --max-time 12 "$search" > "$smoke_dir/restarted.json"
jq -e --slurpfile before "$smoke_dir/normal.json" '.status=="SUCCESS" and ([.offers[]|{stayId,roomTypeId}] == [$before[0].offers[]|{stayId,roomTypeId}])' "$smoke_dir/restarted.json" >/dev/null
echo '통과: OpenAPI, Swagger UI, 정상 검색, A 무응답/B 정상, 전체 실패, 입력 오류, 복구, 재시작 ID 유지'
