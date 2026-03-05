---
name: springdoc-openapi-contract-standardizer
description: Standardize Spring Boot springdoc OpenAPI contracts for frontend consumption. Use when asked to make Swagger UI self-sufficient by normalizing endpoint parameter/response docs and DTO `@Schema` metadata (description/example/constraints/format/nullable/enum semantics), enforce PATCH null semantics, and sync `common-docs/openapi/openapi.v1.json` with verification gates.
---

# Springdoc Openapi Contract Standardizer

## Objective

Standardize API docs only. Do not change business behavior.

Make Swagger UI sufficient for frontend implementation by ensuring:
- every endpoint has explicit request/response/error contract,
- every DTO field has stable meaning and constraints,
- every enum-like field exposes allowed values and value semantics.

## Workflow

1. Lock baseline
- Read `common-docs/PROJECT_PRD.md` and `common-docs/openapi/openapi.v1.json`.
- Inventory DTO and OpenAPI coverage:
```bash
.codex/skills/springdoc-openapi-contract-standardizer/scripts/audit_dto_schema_coverage.sh
.codex/skills/springdoc-openapi-contract-standardizer/scripts/audit_openapi_quality.sh
```

2. Standardize DTO `@Schema`
- Add or refine `@Schema` on every request/response DTO field.
- Fill description, example, requiredMode, min/max/pattern/format/nullable when applicable.
- Keep sensitive fields masked in examples (`password`, `token`).
- Keep date-time examples in UTC Z format (`2026-03-05T01:20:00Z`).

3. Standardize enum-like fields
- Prefer real Java enum types when already modeled in domain/DTO.
- For string code fields, set `allowableValues` and explain each value meaning in description.
- If centralized customizer exists, update its enum-like description/value maps together.

4. Standardize endpoint docs
- Ensure each `/api/v1/**` operation has summary/description.
- Ensure request/response media type contract is explicit (`application/json`) unless binary endpoint.
- Ensure success + validation/auth/permission/domain/500 responses are documented.

5. Handle PATCH semantics explicitly
- For PATCH DTO fields, document null/empty semantics per field.
- Use form: `null이면 변경하지 않습니다` and for arrays include `[]` behavior.

6. Verify and sync snapshot
- Run full backend test:
```bash
cd aini-inu-backend && ./gradlew test
```
- Export OpenAPI snapshot:
```bash
cd aini-inu-backend && ./scripts/export-openapi.sh
```
- Re-run quality gates:
```bash
.codex/skills/springdoc-openapi-contract-standardizer/scripts/audit_openapi_quality.sh
```

## Decision Rules

- Keep API runtime behavior unchanged unless user explicitly requests behavior changes.
- If PRD and runtime conflict, document runtime contract first and report conflict separately.
- Never leave enum-like code fields undocumented.

## Resources

### scripts/
- `scripts/audit_dto_schema_coverage.sh`: DTO-level `@Schema` coverage and missing-description gate
- `scripts/audit_openapi_quality.sh`: OpenAPI snapshot quality gate (description/example/enum/date-time/media type)

### references/
- `references/standardization-rules.md`: strict annotation rules and phrasing conventions
