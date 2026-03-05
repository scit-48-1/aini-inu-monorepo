# Requirements: AINI INU Frontend Contract Refactor

**Defined:** 2026-03-05
**Core Value:** 프론트엔드의 모든 사용자 흐름이 백엔드 Swagger/OpenAPI 계약과 100% 일치하며 크래시 없이 동작해야 한다.

## v1 Requirements

### Contract Baseline

- [ ] **CNTR-01**: User flows use API endpoints whose method/path match runtime OpenAPI exactly.
- [ ] **CNTR-02**: Frontend decodes backend `ApiResponse<T>` envelope consistently across all migrated screens.
- [ ] **CNTR-03**: Frontend maps backend domain error codes to deterministic UI error states.
- [ ] **CNTR-04**: Every core route has contract verification evidence against live backend (`/v3/api-docs`).

### API Layer

- [ ] **APIL-01**: Frontend uses a centralized API layer for all backend-contract HTTP calls.
- [ ] **APIL-02**: User-facing screens do not make ad-hoc direct fetch/axios calls outside the centralized API layer.
- [ ] **APIL-03**: Domain API modules (auth/member/walk/chat/community/pet) expose typed request/response contracts.
- [ ] **APIL-04**: Independent API reads required by one screen can run in parallel safely with deterministic error handling.

### Authentication and Session

- [ ] **AUTH-01**: User can sign up/login/logout with request/response fields aligned to backend contract.
- [ ] **AUTH-02**: User session state survives refresh when backend token/session is valid.
- [ ] **AUTH-03**: User is redirected/recovered predictably on unauthorized responses.
- [ ] **AUTH-04**: Auth screens provide complete loading, validation error, API error, and success transition states.

### Walk Matching and Core Social Flows

- [ ] **WALK-01**: User can browse around-me/walk thread data with contract-aligned query and response parsing.
- [ ] **WALK-02**: Around-me geolocation behavior follows PRD policy (initial one-shot + fallback + manual refresh only).
- [ ] **COMM-01**: User can access feed/story/thread interaction flows with contract-aligned payload handling.
- [ ] **CHAT-01**: User can use chat room list/detail/message flows with contract-aligned pagination and error handling.

### Pet and Profile Flows

- [ ] **PETF-01**: User can create/update/delete pet profile data with backend-aligned payload and validation handling.
- [ ] **PROF-01**: User can view/edit profile and settings screens with contract-aligned member data parsing.
- [ ] **PROF-02**: Profile-related screens provide complete loading, empty, error, and success UX states.

### Stability and Verification

- [ ] **STAB-01**: Core scenario routes run without runtime crash in browser execution.
- [ ] **STAB-02**: Known critical runtime errors identified in exploratory navigation are fixed and regression-protected.
- [ ] **UAT-01**: Agent-browser UAT evidence is captured with screenshots for each major phase gate.
- [ ] **UAT-02**: Final integrated UAT proves end-to-end scenario continuity across auth → matching → profile flows.

## v2 Requirements

### Future Enhancements

- **V2-01**: Contract type generation pipeline automation in CI (strict schema drift blocking).
- **V2-02**: Expanded notification/push UX rollout beyond current in-scope stabilization.
- **V2-03**: Large-scale visual redesign not directly required for contract/stability alignment.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Backend code changes in `aini-inu-backend/` | Backend is frozen and serves as source of truth for contracts. |
| PRD/OpenAPI document edits in `common-docs/` | Docs are read-only reference in this initiative. |
| New non-PRD product features | Current objective is alignment and stabilization, not expansion. |
| Unbounded UI redesign | UX work is limited to state/flow correctness required by PRD and contract behavior. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CNTR-01 | Phase 2 | Pending |
| CNTR-02 | Phase 2 | Pending |
| CNTR-03 | Phase 2 | Pending |
| CNTR-04 | Phase 8 | Pending |
| APIL-01 | Phase 2 | Pending |
| APIL-02 | Phase 2 | Pending |
| APIL-03 | Phase 2 | Pending |
| APIL-04 | Phase 2 | Pending |
| AUTH-01 | Phase 3 | Pending |
| AUTH-02 | Phase 3 | Pending |
| AUTH-03 | Phase 3 | Pending |
| AUTH-04 | Phase 3 | Pending |
| WALK-01 | Phase 4 | Pending |
| WALK-02 | Phase 4 | Pending |
| COMM-01 | Phase 7 | Pending |
| CHAT-01 | Phase 7 | Pending |
| PETF-01 | Phase 5 | Pending |
| PROF-01 | Phase 6 | Pending |
| PROF-02 | Phase 6 | Pending |
| STAB-01 | Phase 8 | Pending |
| STAB-02 | Phase 1 | Pending |
| UAT-01 | Phase 8 | Pending |
| UAT-02 | Phase 8 | Pending |

**Coverage:**
- v1 requirements: 23 total
- Mapped to phases: 23
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-05*
*Last updated: 2026-03-05 after initial definition*
