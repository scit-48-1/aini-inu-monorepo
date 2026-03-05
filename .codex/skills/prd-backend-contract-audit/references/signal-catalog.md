# Signal Catalog

`generate_audit_signals.py` emits candidate findings with `code`.
Use this catalog during triage to reduce false positives.

## Endpoint Surface

- `ENDPOINT_MISSING_IN_OPENAPI`
  - Meaning: controller mapping exists, but endpoint missing in OpenAPI artifact.
  - Typical cause: stale OpenAPI snapshot, conditional controller load, parser miss.
  - Triage: verify runtime `/v3/api-docs` first.

- `OPENAPI_ENDPOINT_WITHOUT_CONTROLLER_MAPPING`
  - Meaning: OpenAPI path/method could not be mapped to parsed controller annotations.
  - Typical cause: parser edge case, generated/indirect endpoints.
  - Triage: confirm with controller grep and runtime route.

## Test Coverage Signals

- `CONTROLLER_ENDPOINT_TEST_MISSING`
  - Meaning: no direct MockMvc/WebMvc path evidence for endpoint.
  - Typical cause: missing slice test, integration-only coverage.
  - Triage: search integration tests before confirming as missing.

- `SERVICE_METHOD_TEST_MISSING`
  - Meaning: public service method name not found in Service/Coverage/Unit tests.
  - Typical cause: untested path, rename mismatch, reflection-based test.
  - Triage: verify by behavior-level test search, not name-only.

## Logical Consistency

- `AUTH_SEED_PASSWORD_INCONSISTENCY`
  - Meaning: auth flow requires password but seed data does not provide it.
  - Typical cause: model upgrade without fixture/schema sync.
  - Triage: confirm with auth code path + seed SQL insert columns.

## Confidence Rules

- Default status from script is conservative:
  - `확정`: deterministic structural mismatch with direct evidence.
  - `의심`: parser/search heuristic may miss edge cases.
- Promote `의심 -> 확정` only after direct source verification.
