# Roadmap: AINI INU Frontend Contract Refactor

## Overview

This roadmap delivers frontend contract correctness and runtime stability in strict dependency order: fix critical crashes first, lock API boundaries, align core user flows by domain, then complete missing in-scope features and finish with integrated verification evidence.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Critical Bug Stabilization** - Remove known crash-level blockers that prevent safe migration work.
- [ ] **Phase 2: API Layer and Contract Alignment** - Centralize transport and enforce method/path/envelope/error contract behavior.
- [ ] **Phase 3: Authentication and Session Alignment** - Make auth/session UX and recovery deterministic against backend contract.
- [ ] **Phase 4: Walk Matching Flow Alignment** - Align around-me and walk-thread behavior including geolocation policy.
- [ ] **Phase 5: Pet Profile Contract Alignment** - Complete contract-safe pet profile create/update/delete flows.
- [ ] **Phase 6: My Page and Settings Alignment** - Align profile/settings data parsing and full UX state transitions.
- [ ] **Phase 7: Missing Core Feature Completion** - Complete remaining in-scope social/chat feature paths.
- [ ] **Phase 8: Final Integration Verification** - Prove end-to-end continuity, stability, and live-contract evidence.

## Phase Details

### Phase 1: Critical Bug Stabilization
**Goal**: Users can navigate core routes without known critical runtime crash patterns blocking further contract migration.
**Depends on**: Nothing (first phase)
**Requirements**: STAB-02
**Success Criteria** (what must be TRUE):
  1. User can execute previously crash-prone exploratory navigation paths without immediate runtime termination.
  2. User sees recoverable fallback/error UI for previously fatal states instead of blank/crashed screens.
  3. Reproducing known critical error paths no longer causes application crash behavior in browser execution.
**Plans**: TBD

### Phase 2: API Layer and Contract Alignment
**Goal**: Backend-contract HTTP behavior is centralized and consistent across migrated screens.
**Depends on**: Phase 1
**Requirements**: CNTR-01, CNTR-02, CNTR-03, APIL-01, APIL-02, APIL-03, APIL-04
**Success Criteria** (what must be TRUE):
  1. User-triggered API actions on migrated screens call backend endpoints with runtime-OpenAPI-matching method/path.
  2. User sees consistent data rendering because migrated screens decode backend `ApiResponse<T>` envelopes uniformly.
  3. User receives deterministic UI error outcomes for backend domain error codes across migrated routes.
  4. Screens that require multiple independent API reads handle partial failures predictably without breaking unrelated data loads.
**Plans**: TBD

### Phase 3: Authentication and Session Alignment
**Goal**: Users can authenticate and recover sessions reliably with complete auth-state UX transitions.
**Depends on**: Phase 2
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04
**Success Criteria** (what must be TRUE):
  1. User can sign up, log in, and log out with request/response handling aligned to backend contract fields.
  2. User session remains valid across refresh when backend token/session is still valid.
  3. User is redirected or recovered consistently when unauthorized responses occur on protected routes.
  4. Auth screens expose full loading, validation error, API error, and success transition states.
**Plans**: TBD

### Phase 4: Walk Matching Flow Alignment
**Goal**: Around-me and walk-thread experiences follow backend contract parsing and PRD geolocation policy.
**Depends on**: Phase 3
**Requirements**: WALK-01, WALK-02
**Success Criteria** (what must be TRUE):
  1. User can browse around-me/walk-thread data with correct contract-aligned query and response behavior.
  2. User experiences PRD-aligned geolocation behavior: initial one-shot lookup, fallback handling, and no automatic loop refresh.
  3. User can manually refresh location-based results and receives deterministic loading/error/success transitions.
**Plans**: TBD

### Phase 5: Pet Profile Contract Alignment
**Goal**: Users can fully manage pet profile data using backend-aligned payload and validation handling.
**Depends on**: Phase 4
**Requirements**: PETF-01
**Success Criteria** (what must be TRUE):
  1. User can create pet profile entries with backend-aligned fields and clear validation feedback.
  2. User can edit pet profile data and observe persisted contract-safe updates after reload.
  3. User can delete pet profiles and receive deterministic success/error handling without screen breakage.
**Plans**: TBD

### Phase 6: My Page and Settings Alignment
**Goal**: Users can view and manage my/settings data with contract-safe parsing and complete UX state coverage.
**Depends on**: Phase 5
**Requirements**: PROF-01, PROF-02
**Success Criteria** (what must be TRUE):
  1. User can view my/settings screens with correctly parsed member profile data from backend responses.
  2. User can edit profile/settings fields and receive contract-aligned save results.
  3. Profile-related routes consistently show loading, empty, error, and success states.
**Plans**: TBD

### Phase 7: Missing Core Feature Completion
**Goal**: Remaining in-scope social and chat feature paths are completed with contract-aligned behavior.
**Depends on**: Phase 6
**Requirements**: COMM-01, CHAT-01
**Success Criteria** (what must be TRUE):
  1. User can execute feed/story/thread interaction flows with contract-aligned payload parsing.
  2. User can use chat room list/detail/message flows with contract-aligned pagination handling.
  3. User receives deterministic error/retry handling in chat and social interactions.
  4. Missing v1 in-scope feature paths are available without relying on placeholder-only behavior.
**Plans**: TBD

### Phase 8: Final Integration Verification
**Goal**: Integrated auth-to-matching-to-profile journeys are contract-verified, stable, and evidenced.
**Depends on**: Phase 7
**Requirements**: CNTR-04, STAB-01, UAT-01, UAT-02
**Success Criteria** (what must be TRUE):
  1. User can complete end-to-end auth -> matching -> profile scenarios without runtime crash.
  2. Each core route has live-backend contract verification evidence and associated UAT screenshot artifacts.
  3. Final integrated UAT demonstrates continuity across phase-delivered flows with no broken handoff states.
  4. Core scenario routes remain stable during integrated regression execution.
**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Critical Bug Stabilization | 0/1 | Not started | - |
| 2. API Layer and Contract Alignment | 0/3 | Not started | - |
| 3. Authentication and Session Alignment | 0/2 | Not started | - |
| 4. Walk Matching Flow Alignment | 0/2 | Not started | - |
| 5. Pet Profile Contract Alignment | 0/1 | Not started | - |
| 6. My Page and Settings Alignment | 0/2 | Not started | - |
| 7. Missing Core Feature Completion | 0/2 | Not started | - |
| 8. Final Integration Verification | 0/2 | Not started | - |
