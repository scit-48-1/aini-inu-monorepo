# AGENTS.md

## 1. Repository Context
- This repository is a monorepo with three main modules:
  - `aini-inu-backend`
  - `aini-inu-frontend`
  - `common-docs`
- Current delivery center is `aini-inu-backend` + `common-docs`.
- `aini-inu-frontend` is in a pre-refactor state and will undergo major restructuring.

## 2. Priority Order (Current)
1. Backend contract correctness and stability
2. `common-docs` synchronization (PRD/OpenAPI)
3. Frontend refactor readiness and safe incremental cleanup

## 3. Source of Truth
- Product/feature policy: `common-docs/PROJECT_PRD.md`
- API contract snapshot: `common-docs/openapi/openapi.v1.json`
- Runtime API implementation: `aini-inu-backend/src/main/java/**`
- Data model baseline: `aini-inu-backend/src/main/resources/db/ddl/**`

If there is any mismatch, resolve in this order:
1. `PROJECT_PRD.md`
2. OpenAPI contract
3. Backend implementation
4. Frontend adaptation

## 4. Module-Specific Working Rules

### Backend (`aini-inu-backend`)
- Preserve domain-first package structure and existing response envelope patterns (`ApiResponse<T>`).
- Any API change must include:
  - controller/service/repository or equivalent layer updates
  - DTO/schema updates
  - test updates (unit/slice/integration/contract where applicable)
- Keep Entity/DDL consistency. Do not modify one without checking the other.

### Docs (`common-docs`)
- Treat docs as executable contract, not optional notes.
- When behavior/contract changes:
  - update `PROJECT_PRD.md` wording where policy changed
  - update OpenAPI snapshot and related references
- Keep terminology locked (e.g., Story vs WalkDiary) as defined in PRD.

### Frontend (`aini-inu-frontend`, major refactor pending)
- Prefer changes that reduce refactor risk:
  - isolate domain logic into hooks/services
  - reduce page-level coupling
  - align API assumptions with backend/OpenAPI
- Avoid large feature expansion on legacy structures unless it is a blocker fix.
- For unavoidable legacy edits, leave clear refactor boundary notes in code comments or PR summary.

## 5. Default Agent Flow (`/init` and task start)
1. Read `common-docs/PROJECT_PRD.md` for requirement and policy context.
2. Check relevant backend domain and existing tests first.
3. Determine if frontend changes are necessary; if not necessary, do not touch frontend.
4. Apply minimal, contract-safe change set.
5. Run validations.
6. Report:
  - contract impact
  - docs sync status
  - test/build results

## 6. Validation Commands
- Backend tests: `cd aini-inu-backend && ./gradlew test`
- Backend run: `cd aini-inu-backend && ./gradlew bootRun`
- OpenAPI export: `cd aini-inu-backend && ./scripts/export-openapi.sh`
- Frontend lint: `cd aini-inu-frontend && npm run lint`
- Frontend build: `cd aini-inu-frontend && npm run build`

## 7. Guardrails
- Never change API contract silently. Sync code, tests, and docs in the same workstream.
- Never prioritize frontend cosmetic work over backend contract integrity.
- If requirements are ambiguous, clarify PRD/contract intent before implementing.
