---
name: prd-backend-contract-audit
description: Audit alignment between `common-docs/PROJECT_PRD.md` and the backend implementation (`aini-inu-backend`), test code, and Swagger/OpenAPI runtime spec. Use when asked to find and document `계약상 불일치`, `미구현`, `초과구현`, `논리적 어긋남`, especially when the result must be saved as a report file under `common-docs`.
---

# Prd Backend Contract Audit

## Overview

Execute a deterministic PRD-contract audit workflow for backend code, tests, and Swagger/OpenAPI.  
Produce evidence-backed findings and save a report markdown file in `common-docs`.

## Workflow

### 1) Gather Baseline Inputs

- Read baseline docs:
  - `common-docs/PROJECT_PRD.md`
  - (Optional for cross-check) `common-docs/API_SPEC.md`, `common-docs/FEATURE_SPEC.md`
- Confirm target code scope:
  - `aini-inu-backend/src/main/java`
  - `aini-inu-backend/src/test/java`
- If available, capture branch/commit for traceability.

### 2) Collect Runtime Evidence

Run the bundled script:

```bash
.codex/skills/prd-backend-contract-audit/scripts/collect_audit_artifacts.sh
```

The script does both:
- Re-runs backend tests (`./gradlew test --no-daemon --rerun-tasks`)
- Boots backend and captures runtime OpenAPI from `/v3/api-docs`

Artifacts are written to `/tmp/aini_contract_audit` by default.

If sandbox blocks execution, re-run with escalated permissions.

### 3) Perform Contract Audit

Use `references/checklist.md` to classify every finding into:
- `계약상 불일치`
- `미구현`
- `초과구현`
- `논리적 어긋남`

For each finding, include:
- Severity: `Critical|High|Medium|Low`
- Status: `확정|의심`
- Evidence: concrete file path and line, endpoint, schema field, test name, or OpenAPI path

### 4) Write Report in `common-docs`

Start from `references/report-template.md` and fill actual findings.

Default naming rule:
- `common-docs/PRD_BACKEND_SWAGGER_AUDIT_YYYY-MM-DD.md`

Required sections:
- Scope and baseline
- Execution results (tests/OpenAPI extraction)
- Issue summary table
- Detailed findings
- Matched/aligned items
- Evidence files/paths

### 5) Final Verification

Before delivering:
- Ensure every claim has evidence.
- Mark low-confidence claims as `의심`.
- Distinguish document mismatch vs implementation bug vs intentionally added feature.
- Confirm the report file exists under `common-docs`.

## Resources

### scripts/
- `scripts/collect_audit_artifacts.sh`
  - Runs tests
  - Extracts runtime OpenAPI
  - Writes logs and artifacts under `/tmp/aini_contract_audit`

### references/
- `references/checklist.md`: classification and decision rules
- `references/report-template.md`: output markdown structure

## Constraints
- Do not claim pass/fail without command output evidence.
- Do not treat Swagger annotation text alone as source of truth if runtime spec conflicts.
- Prefer runtime OpenAPI (`/v3/api-docs`) over static assumptions.
- Keep report factual; avoid remediation plans unless user asks.
