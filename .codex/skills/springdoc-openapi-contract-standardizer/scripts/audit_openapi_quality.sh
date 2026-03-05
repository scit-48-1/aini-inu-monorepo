#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
SNAPSHOT_PATH="${1:-${REPO_ROOT}/common-docs/openapi/openapi.v1.json}"

if [[ ! -f "${SNAPSHOT_PATH}" ]]; then
  echo "[ERROR] OpenAPI snapshot not found: ${SNAPSHOT_PATH}" >&2
  exit 2
fi

query_missing_desc='
.components.schemas
| to_entries[]
| .key as $schema
| (.value.properties // {})
| to_entries[]
| select((.value.description // "") == "")
| "\($schema).\(.key)"
'

query_missing_example='
.components.schemas
| to_entries[]
| .key as $schema
| (.value.properties // {})
| to_entries[]
| select((.value.example == null) and ((.key | ascii_downcase | test("password|token")) | not))
| "\($schema).\(.key)"
'

query_enum_like_without_enum='
.components.schemas
| to_entries[]
| .key as $schema
| (.value.properties // {})
| to_entries[]
| select(
    ((.value.type // "") == "string")
    and (.key | test("(Type|Status|Action|State)$"))
    and (((.value.enum // []) | length) == 0)
    and (.key != "contentType")
  )
| "\($schema).\(.key)"
'

query_non_utc_datetime='
.components.schemas
| to_entries[]
| .key as $schema
| (.value.properties // {})
| to_entries[]
| select(
    (.value.format // "") == "date-time"
    and (.value.example != null)
    and ((.value.example | type) == "string")
    and ((.value.example | endswith("Z")) | not)
  )
| "\($schema).\(.key)=\(.value.example)"
'

query_missing_summary='
.paths
| to_entries[]
| select(.key | startswith("/api/v1/"))
| .key as $path
| .value
| to_entries[]
| select(.key | test("^(get|post|put|patch|delete|options|head|trace)$"))
| select((.value.summary // "") == "")
| "\($path) \(.key)"
'

query_missing_description='
.paths
| to_entries[]
| select(.key | startswith("/api/v1/"))
| .key as $path
| .value
| to_entries[]
| select(.key | test("^(get|post|put|patch|delete|options|head|trace)$"))
| select((.value.description // "") == "")
| "\($path) \(.key)"
'

query_missing_json_request='
.paths
| to_entries[]
| select(.key | startswith("/api/v1/"))
| .key as $path
| .value
| to_entries[]
| select(.key | test("^(get|post|put|patch|delete|options|head|trace)$"))
| select(.value.requestBody != null)
| select(
    (((.value.requestBody.content // {}) | keys | index("application/json")) == null)
    and (
      (((.value.requestBody.content // {}) | keys)
      | map(test("application/octet-stream|image/(jpeg|png|webp)"))
      | any) | not
    )
  )
| "\($path) \(.key)"
'

query_missing_json_response='
.paths
| to_entries[]
| select(.key | startswith("/api/v1/"))
| .key as $path
| .value
| to_entries[]
| select(.key | test("^(get|post|put|patch|delete|options|head|trace)$"))
| .key as $method
| (.value.responses // {})
| to_entries[]
| .key as $status
| select(.value.content != null)
| select(
    (((.value.content // {}) | keys | index("application/json")) == null)
    and (
      (((.value.content // {}) | keys)
      | map(test("application/octet-stream|image/(jpeg|png|webp)"))
      | any) | not
    )
  )
| "\($path) \($method) \($status)"
'

missing_desc="$(jq -r "${query_missing_desc}" "${SNAPSHOT_PATH}")"
missing_example="$(jq -r "${query_missing_example}" "${SNAPSHOT_PATH}")"
enum_like_without_enum="$(jq -r "${query_enum_like_without_enum}" "${SNAPSHOT_PATH}")"
non_utc_datetime="$(jq -r "${query_non_utc_datetime}" "${SNAPSHOT_PATH}")"
missing_summary="$(jq -r "${query_missing_summary}" "${SNAPSHOT_PATH}")"
missing_description="$(jq -r "${query_missing_description}" "${SNAPSHOT_PATH}")"
missing_json_request="$(jq -r "${query_missing_json_request}" "${SNAPSHOT_PATH}")"
missing_json_response="$(jq -r "${query_missing_json_response}" "${SNAPSHOT_PATH}")"

count_lines() {
  local text="${1:-}"
  if [[ -z "${text}" ]]; then
    echo 0
  else
    printf '%s\n' "${text}" | sed '/^$/d' | wc -l | tr -d ' '
  fi
}

missing_desc_count="$(count_lines "${missing_desc}")"
missing_example_count="$(count_lines "${missing_example}")"
enum_like_without_enum_count="$(count_lines "${enum_like_without_enum}")"
non_utc_datetime_count="$(count_lines "${non_utc_datetime}")"
missing_summary_count="$(count_lines "${missing_summary}")"
missing_description_count="$(count_lines "${missing_description}")"
missing_json_request_count="$(count_lines "${missing_json_request}")"
missing_json_response_count="$(count_lines "${missing_json_response}")"

echo "snapshot=${SNAPSHOT_PATH}"
echo "missing_property_description=${missing_desc_count}"
echo "missing_property_example_except_sensitive=${missing_example_count}"
echo "enum_like_string_without_enum=${enum_like_without_enum_count}"
echo "non_utc_datetime_example=${non_utc_datetime_count}"
echo "missing_operation_summary=/api/v1/** ${missing_summary_count}"
echo "missing_operation_description=/api/v1/** ${missing_description_count}"
echo "missing_request_application_json=/api/v1/** ${missing_json_request_count}"
echo "missing_response_application_json=/api/v1/** ${missing_json_response_count}"

failed=0

print_fail_block() {
  local title="$1"
  local content="$2"
  local max_lines="${3:-120}"
  echo ""
  echo "[FAIL] ${title}"
  printf '%s\n' "${content}" | sed '/^$/d' | sed -n "1,${max_lines}p"
}

if [[ "${missing_desc_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Missing property description" "${missing_desc}" 160
fi

if [[ "${missing_example_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Missing property example (except sensitive)" "${missing_example}" 160
fi

if [[ "${enum_like_without_enum_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Enum-like string without enum list" "${enum_like_without_enum}" 160
fi

if [[ "${non_utc_datetime_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Non-UTC date-time example" "${non_utc_datetime}" 120
fi

if [[ "${missing_summary_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Missing operation summary" "${missing_summary}" 120
fi

if [[ "${missing_description_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Missing operation description" "${missing_description}" 120
fi

if [[ "${missing_json_request_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Missing request application/json" "${missing_json_request}" 120
fi

if [[ "${missing_json_response_count}" -gt 0 ]]; then
  failed=1
  print_fail_block "Missing response application/json" "${missing_json_response}" 120
fi

if [[ ${failed} -eq 1 ]]; then
  exit 1
fi

echo ""
echo "[OK] OpenAPI quality gate passed."
