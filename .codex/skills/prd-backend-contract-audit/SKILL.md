---
name: prd-backend-contract-audit
description: Detect contract gaps and missing coverage across PRD, backend implementation, tests, and OpenAPI. Use when asked to comprehensively find `계약상 불일치`, `미구현`, `초과구현`, `논리적 어긋남` and produce evidence-backed reports.
---

# PRD Backend Contract Audit

## Objective

Run a **detect-only** audit that finds as many high-confidence gaps as possible across:
- PRD policy (`common-docs/PROJECT_PRD.md`)
- Runtime/snapshot OpenAPI
- Backend controller/service/repository implementation
- Backend tests

This skill is for **finding** issues, not fixing them.

## Execution Mode

- Default mode: `detect-only` (no source edits outside report output).
- Allowed outputs:
  - `/tmp/aini_contract_audit/*` artifacts
  - report markdown under `common-docs/`
- If user asks implementation, switch to a separate workstream after audit closure.

## Workflow (Mandatory)

### 1) Baseline Lock
- Read:
  - `common-docs/PROJECT_PRD.md`
  - `common-docs/openapi/openapi.v1.json`
- Capture branch + commit (`git rev-parse --short HEAD`).
- Define scope explicitly:
  - default is full backend + PRD/OpenAPI contracts.

### 2) Runtime Evidence Collection

Run:

```bash
.codex/skills/prd-backend-contract-audit/scripts/collect_audit_artifacts.sh
```

What it collects:
- full backend test run log
- runtime OpenAPI (`/v3/api-docs`)
- automatic broad signal artifacts:
  - `/tmp/aini_contract_audit/signals.json`
  - `/tmp/aini_contract_audit/signals.md`

### 3) Automated Wide Scan (Required)

Use auto signals as *candidate findings*, not final verdicts:

```bash
python3 .codex/skills/prd-backend-contract-audit/scripts/generate_audit_signals.py \
  --root . \
  --out-dir /tmp/aini_contract_audit
```

This scan covers:
- controller endpoint vs OpenAPI presence
- endpoint direct-test evidence gaps
- service public-method test reference gaps
- auth-flow vs seed-data consistency signals

### 4) Manual Triage + Classification

Apply `references/checklist.md` and keep one primary type:
- `계약상 불일치`
- `미구현`
- `초과구현`
- `논리적 어긋남`

For each finding, include:
- Severity (`Critical|High|Medium|Low`)
- Status (`확정|의심`)
- Evidence with concrete path/line or endpoint/schema path.

### 5) Report Output

Create:
- `common-docs/PRD_BACKEND_SWAGGER_AUDIT_YYYY-MM-DD.md`

Use `references/report-template.md` and include:
- execution evidence
- summary table
- detailed findings
- explicit `확정` vs `의심`
- aligned/verified items

## Resources

### scripts/
- `scripts/collect_audit_artifacts.sh`
  - tests + runtime OpenAPI + signal generation
- `scripts/generate_audit_signals.py`
  - broad static/rule-based gap signal generation

### references/
- `references/checklist.md`
- `references/report-template.md`
- `references/signal-catalog.md`

## Guardrails
- Never assert a finding without evidence.
- Treat automated signal output as draft candidates until triaged.
- Runtime OpenAPI outranks annotation assumptions.
- Distinguish:
  - contract mismatch vs missing implementation vs missing tests.
- Avoid remediation proposals unless user requests them.
