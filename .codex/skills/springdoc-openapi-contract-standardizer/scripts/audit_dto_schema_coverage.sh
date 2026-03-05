#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
BACKEND_DIR="${BACKEND_DIR:-${REPO_ROOT}/aini-inu-backend}"
DTO_ROOT="${BACKEND_DIR}/src/main/java/scit/ainiinu"

if [[ ! -d "${DTO_ROOT}" ]]; then
  echo "[ERROR] DTO root not found: ${DTO_ROOT}" >&2
  exit 2
fi

dto_files=()
while IFS= read -r line; do
  dto_files+=("${line}")
done < <(rg --files "${DTO_ROOT}" | rg '/dto/.*\.java$' || true)

if [[ ${#dto_files[@]} -eq 0 ]]; then
  echo "[ERROR] No DTO files found under: ${DTO_ROOT}" >&2
  exit 2
fi

dto_without_schema=()
while IFS= read -r line; do
  dto_without_schema+=("${line}")
done < <(rg --files-without-match '@Schema\(' "${dto_files[@]}" || true)
schema_without_description="$(rg -nP '@Schema\((?s)(?![^)]*description)' -U "${DTO_ROOT}" | rg '/dto/' || true)"

echo "dto_files_total=${#dto_files[@]}"
echo "dto_without_any_schema=${#dto_without_schema[@]}"
if [[ -n "${schema_without_description}" ]]; then
  schema_without_description_count="$(printf '%s\n' "${schema_without_description}" | sed '/^$/d' | wc -l | tr -d ' ')"
else
  schema_without_description_count=0
fi
echo "schema_annotations_without_description=${schema_without_description_count}"

failed=0

if [[ ${#dto_without_schema[@]} -gt 0 ]]; then
  failed=1
  echo ""
  echo "[FAIL] DTO files without @Schema"
  printf '%s\n' "${dto_without_schema[@]}" | sed -n '1,120p'
fi

if [[ "${schema_without_description_count}" -gt 0 ]]; then
  failed=1
  echo ""
  echo "[FAIL] @Schema annotations without description"
  printf '%s\n' "${schema_without_description}" | sed -n '1,160p'
fi

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo ""
echo "[OK] DTO @Schema coverage gate passed."
