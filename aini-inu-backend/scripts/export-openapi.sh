#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

PORT=18083
WAIT_SECONDS=150
OUT_PATH="$ROOT_DIR/../common-docs/openapi/openapi.v1.json"
BOOT_LOG="/tmp/aini_openapi_boot.log"

BOOT_PID=""
RAW_TMP=""
PRETTY_TMP=""
STARTED_BY_SCRIPT=0

usage() {
  cat <<'USAGE'
Usage: ./scripts/export-openapi.sh [options]

Options:
  --port <port>            Port to probe/boot backend (default: 18083)
  --out <path>             Output json path (default: ../common-docs/openapi/openapi.v1.json)
  --wait-seconds <seconds> Wait timeout for /v3/api-docs (default: 150)
  --boot-log <path>        Boot log path when script starts backend (default: /tmp/aini_openapi_boot.log)
  -h, --help               Show this help
USAGE
}

cleanup() {
  if [[ -n "${BOOT_PID}" ]] && [[ "${STARTED_BY_SCRIPT}" -eq 1 ]]; then
    if kill -0 "${BOOT_PID}" >/dev/null 2>&1; then
      kill "${BOOT_PID}" >/dev/null 2>&1 || true
      wait "${BOOT_PID}" >/dev/null 2>&1 || true
    fi
  fi

  if [[ -n "${RAW_TMP}" ]]; then
    rm -f "${RAW_TMP}" >/dev/null 2>&1 || true
  fi
  if [[ -n "${PRETTY_TMP}" ]]; then
    rm -f "${PRETTY_TMP}" >/dev/null 2>&1 || true
  fi
}

log() {
  printf '[openapi-export] %s\n' "$1"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "missing required command: $1" >&2
    exit 1
  fi
}

fetch_openapi() {
  local target="$1"
  curl -sf "http://localhost:${PORT}/v3/api-docs" >"${target}"
}

wait_for_openapi() {
  local target="$1"
  local i
  for ((i=1; i<=WAIT_SECONDS; i++)); do
    if fetch_openapi "${target}"; then
      return 0
    fi
    sleep 1
  done
  return 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --port)
      PORT="$2"
      shift 2
      ;;
    --out)
      OUT_PATH="$2"
      shift 2
      ;;
    --wait-seconds)
      WAIT_SECONDS="$2"
      shift 2
      ;;
    --boot-log)
      BOOT_LOG="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

require_cmd curl
require_cmd python3

OUT_DIR="$(dirname "${OUT_PATH}")"
mkdir -p "${OUT_DIR}"

RAW_TMP="$(mktemp "${OUT_DIR}/openapi.raw.XXXXXX.json")"
PRETTY_TMP="$(mktemp "${OUT_DIR}/openapi.pretty.XXXXXX.json")"

trap cleanup EXIT

if fetch_openapi "${RAW_TMP}"; then
  log "reused running backend on port ${PORT}"
else
  : >"${BOOT_LOG}"
  BOOT_ARGS="--server.port=${PORT} --spring.ai.model.embedding.text=none --spring.autoconfigure.exclude=org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingAutoConfiguration,org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration,org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration"

  log "starting backend with bootRun on port ${PORT}"
  (
    cd "${ROOT_DIR}"
    ./gradlew bootRun --args="${BOOT_ARGS}"
  ) >"${BOOT_LOG}" 2>&1 &

  BOOT_PID=$!
  STARTED_BY_SCRIPT=1

  if ! wait_for_openapi "${RAW_TMP}"; then
    echo "failed to collect /v3/api-docs within ${WAIT_SECONDS}s" >&2
    echo "boot log: ${BOOT_LOG}" >&2
    exit 1
  fi
fi

python3 -m json.tool "${RAW_TMP}" > "${PRETTY_TMP}"
mv "${PRETTY_TMP}" "${OUT_PATH}"

BYTES="$(wc -c < "${OUT_PATH}" | tr -d ' ')"
log "openapi snapshot updated: ${OUT_PATH} (${BYTES} bytes)"
