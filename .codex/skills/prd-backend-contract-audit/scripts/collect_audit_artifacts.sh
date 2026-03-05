#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${ROOT_DIR:-$(pwd)}"
BACKEND_DIR="${BACKEND_DIR:-$ROOT_DIR/aini-inu-backend}"
OUT_DIR="${OUT_DIR:-/tmp/aini_contract_audit}"
PORT="${PORT:-18080}"
WAIT_SECONDS="${WAIT_SECONDS:-120}"

OPENAPI_RAW="$OUT_DIR/openapi_raw.json"
OPENAPI_PRETTY="$OUT_DIR/openapi_pretty.json"
BOOT_LOG="$OUT_DIR/boot.log"
TEST_LOG="$OUT_DIR/test.log"
STATUS_FILE="$OUT_DIR/status.txt"
SIGNALS_JSON="$OUT_DIR/signals.json"
SIGNALS_MD="$OUT_DIR/signals.md"
SIGNAL_SCRIPT="$ROOT_DIR/.codex/skills/prd-backend-contract-audit/scripts/generate_audit_signals.py"

mkdir -p "$OUT_DIR"

log() {
  printf '[audit] %s\n' "$1"
}

run_tests() {
  log "running backend tests"
  (
    cd "$BACKEND_DIR"
    ./gradlew test --no-daemon --rerun-tasks
  ) >"$TEST_LOG" 2>&1
  log "tests completed (log: $TEST_LOG)"
}

wait_for_openapi() {
  local i=0
  while [ "$i" -lt "$WAIT_SECONDS" ]; do
    if curl -sf "http://localhost:${PORT}/v3/api-docs" >"$OPENAPI_RAW"; then
      return 0
    fi
    i=$((i + 1))
    sleep 1
  done
  return 1
}

boot_and_capture() {
  local run_args="$1"
  local pid=""

  : >"$BOOT_LOG"

  (
    cd "$BACKEND_DIR"
    ./gradlew bootRun --args="$run_args"
  ) >"$BOOT_LOG" 2>&1 &
  pid=$!

  if wait_for_openapi; then
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" >/dev/null 2>&1 || true
    return 0
  fi

  kill "$pid" >/dev/null 2>&1 || true
  wait "$pid" >/dev/null 2>&1 || true
  return 1
}

extract_openapi() {
  log "extracting runtime OpenAPI on port ${PORT}"

  local args_primary="--spring.profiles.active=test --server.port=${PORT} --spring.ai.google.genai.project-id=dummy"
  local args_fallback="--server.port=${PORT} --spring.ai.model.embedding.text=none --spring.autoconfigure.exclude=org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingAutoConfiguration,org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration,org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration"

  if boot_and_capture "$args_primary"; then
    log "openapi capture succeeded with primary boot args"
  elif boot_and_capture "$args_fallback"; then
    log "openapi capture succeeded with fallback boot args"
  else
    log "openapi capture failed (boot log: $BOOT_LOG)"
    return 1
  fi

  python3 -m json.tool "$OPENAPI_RAW" >"$OPENAPI_PRETTY"
  log "openapi artifacts: $OPENAPI_RAW, $OPENAPI_PRETTY"
}

generate_signals() {
  if [ ! -f "$SIGNAL_SCRIPT" ]; then
    log "signal script not found, skip: $SIGNAL_SCRIPT"
    return 0
  fi
  log "generating broad audit signals"
  python3 "$SIGNAL_SCRIPT" --root "$ROOT_DIR" --out-dir "$OUT_DIR"
}

write_status() {
  {
    echo "timestamp=$(date '+%Y-%m-%d %H:%M:%S %z')"
    echo "backend_dir=$BACKEND_DIR"
    echo "out_dir=$OUT_DIR"
    echo "openapi_raw=$OPENAPI_RAW"
    echo "openapi_pretty=$OPENAPI_PRETTY"
    echo "boot_log=$BOOT_LOG"
    echo "test_log=$TEST_LOG"
    echo "test_report=$BACKEND_DIR/build/reports/tests/test/index.html"
    echo "signals_json=$SIGNALS_JSON"
    echo "signals_md=$SIGNALS_MD"
  } >"$STATUS_FILE"
}

main() {
  if [ ! -d "$BACKEND_DIR" ]; then
    echo "backend directory not found: $BACKEND_DIR" >&2
    exit 1
  fi

  run_tests
  extract_openapi
  generate_signals
  write_status
  log "artifact collection complete (status: $STATUS_FILE)"
}

main "$@"
