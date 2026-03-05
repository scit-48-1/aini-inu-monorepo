# Frontend Refactor Architecture: Centralized API Layer and Contract-Safe Flows

## 1. Goal and Guardrails

This document defines a refactor architecture for `aini-inu-frontend` that centralizes API access and keeps frontend behavior contract-safe against backend and docs.

Guardrails from current project context:
- Priority order: backend contract stability -> docs sync -> frontend readiness.
- Source of truth order: `PROJECT_PRD.md` -> OpenAPI snapshot -> backend runtime -> frontend adaptation.
- No silent contract drift: frontend must adapt to backend contract, not redefine it.
- Refactor should be incremental and low-risk, not a big-bang rewrite.

## 2. Current State Snapshot (from provided context)

Observed from `src/services/api/apiClient.ts` and usage graph:
- Most internal API calls already route through `apiClient` with `API_BASE_URL = /api/v1`.
- `apiClient` assumes a response envelope (`success`, `data`, `message`) and returns `data`.
- Error handling is generic and string-based; domain-level error classes are not defined.
- Route pages and hooks call domain services directly (`memberService`, `threadService`, `postService`, `chatService`).
- Some calls bypass central client:
  - `locationService` uses direct external `fetch` (Nominatim).
  - `geminiService` uses hardcoded absolute URL (`http://localhost:8080/api/v1/...`).
- Mocks are tightly coupled to `/api/v1/*` and envelope behavior (MSW handlers).

Main architectural gap:
- There is an API client, but not a full centralized contract layer with strict boundaries, typed endpoint definitions, decode/validation policy, and migration seams.

## 3. Target Architecture (layered + feature aligned)

### 3.1 Layer Boundaries

1. UI Layer (`src/app`, `src/components`)
- Responsibility: rendering, local UI state, user intent.
- Must not call `fetch` or API client directly.
- Talks only to feature hooks/actions.

2. Feature Orchestration Layer (`src/features/*/hooks`, `src/features/*/actions`)
- Responsibility: screen use-cases, loading/error/success transitions, optimistic updates.
- Talks only to feature API facade (application layer), never to raw transport.

3. API Facade Layer (`src/api/modules/*`)
- Responsibility: business endpoint grouping (`auth`, `member`, `thread`, `post`, `chat`).
- Converts DTOs to UI-safe models when needed.
- Enforces endpoint constants and request/response typing.

4. Transport Layer (`src/api/transport/*`)
- Responsibility: HTTP transport, headers, timeout, abort, retry policy (if any), envelope decoding, error mapping.
- Single place where network protocol decisions live.

5. Contract Layer (`src/api/contracts/*`)
- Responsibility: OpenAPI-aligned types, endpoint path/method constants, optional runtime schema validators.
- Defines canonical API DTO types separate from view models.

6. Integration Adapters (`src/api/integrations/*`)
- Responsibility: external APIs not under backend contract (geocoding, AI proxy if external).
- Kept separate from backend-contract modules to avoid contract contamination.

### 3.2 Recommended Folder Skeleton

```text
src/
  api/
    contracts/
      endpoints.ts
      dto/
        auth.ts
        member.ts
        thread.ts
        post.ts
        chat.ts
    transport/
      httpClient.ts
      envelope.ts
      errors.ts
      query.ts
      authHeader.ts
    modules/
      authApi.ts
      memberApi.ts
      threadApi.ts
      postApi.ts
      chatApi.ts
    integrations/
      geocodingApi.ts
      aiAnalysisApi.ts
    index.ts
  features/
    auth/
    dashboard/
    feed/
    around-me/
    chat/
    profile/
```

## 4. Contract-Safe Data Flow

### 4.1 Request Flow

1. UI event in page/component.
2. Feature hook/action builds use-case input (view model).
3. Facade method maps view model -> request DTO.
4. Transport executes request via endpoint constant (`method + path template`).
5. Transport injects common options:
- base URL (`/api/v1`)
- JSON headers
- auth header policy
- timeout + abort handling
- query parameter serializer

### 4.2 Response Flow

1. Transport receives response.
2. Envelope parser validates expected backend envelope shape.
3. Non-2xx or `success=false` is converted into typed `ApiError`.
4. Facade maps DTO -> UI model.
5. Feature hook updates page state (`idle/loading/success/error`) and exposes stable UI contract.

### 4.3 Error Taxonomy

Use explicit error classes instead of raw `Error` strings:
- `ApiNetworkError`
- `ApiTimeoutError`
- `ApiUnauthorizedError`
- `ApiForbiddenError`
- `ApiNotFoundError`
- `ApiValidationError`
- `ApiServerError`
- `ApiContractError` (invalid envelope/schema mismatch)

Benefits:
- deterministic UI behavior
- clearer retry/fallback logic
- easier telemetry and debugging

## 5. Component and Ownership Boundaries

Boundary rules to enforce during refactor:
- Pages may import feature hooks, not `services/api/*`.
- Shared components are presentation-only and receive data via props.
- Zustand stores may use feature actions/facades, not transport.
- Only transport may call `fetch` for backend contract APIs.
- External integrations (`location`, `ai`) remain isolated adapters and do not leak into backend contract modules.

Immediate coupling hotspots to migrate first:
- `app/dashboard/page.tsx` (multi-domain fetch + partial failure handling).
- `hooks/useRadarLogic.ts` (high traffic + polling + thread/member/location mix).
- `app/feed/page.tsx` and `hooks/useMemberProfile.ts` (cross-domain composition).
- `app/chat/[id]/page.tsx` (room/messages/profile/diary composed flow).

## 6. Migration Seams (incremental, safe)

Seam A: Compatibility wrapper
- Keep existing `memberService`, `threadService`, `postService`, `chatService`, `authService` export shape.
- Re-implement internals to call new `api/modules/*`.
- This avoids immediate page-level churn.

Seam B: Endpoint constant adoption
- Replace inline path strings with constants in `api/contracts/endpoints.ts`.
- Start with read-only endpoints first (`getMe`, `getThreads`, `getPosts`, `getRooms`).

Seam C: Error policy migration
- Introduce typed errors in transport.
- Adapt existing toast/UI handlers gradually with compatibility mapping.

Seam D: External call normalization
- Move `geminiService` hardcoded URL behind `integrations/aiAnalysisApi.ts`.
- Keep `locationService` as integration adapter with same timeout/error contract shape.

Seam E: Hook-level extraction
- Move heavy page logic into feature hooks (`useDashboardData`, `useFeedData`, `useChatRoomData`, `useAroundMeData`).
- Route pages become composition-only.

Seam F: Mock and contract sync
- Update MSW handlers to mirror endpoint constants and envelope/error taxonomy.
- Prevent mock/backend divergence during migration.

## 7. Suggested Build Order

Phase 0: Contract baseline and inventory
- Build endpoint matrix from OpenAPI and current service calls.
- Mark mismatches (path/method/DTO/envelope assumptions).
- Output: migration checklist per endpoint.

Phase 1: Transport core
- Create `transport/httpClient.ts`, `envelope.ts`, `errors.ts`, `query.ts`.
- Add compatibility layer so old services keep working.
- Output: all existing services can run through new transport without UI changes.

Phase 2: Auth + Member vertical slice
- Migrate `authService` and `memberService` to new module facades.
- Update `useUserStore`, `useProfile`, login/signup flow to typed errors.
- Validate login/signup/profile update/follower flows.

Phase 3: Thread + Feed vertical slice
- Migrate `threadService`, `postService` to endpoint constants and DTO mapping.
- Refactor dashboard/feed/around-me data hooks for consistent state transitions.
- Validate create/update/delete/join/comment/like flows.

Phase 4: Chat vertical slice
- Migrate `chatService` and chat room/message flows.
- Normalize polling or later upgrade path for realtime transport.
- Validate room creation, message send, room fetch consistency.

Phase 5: Integration adapters
- Move direct external calls (`location`, `ai`) under integration boundary.
- Remove hardcoded backend URL and use environment-driven base settings.

Phase 6: Boundary enforcement and cleanup
- Remove direct `services/api/*` imports from pages.
- Add lint rule or import restriction for `fetch` and raw service access outside allowed layers.
- Decommission legacy service code paths after parity is proven.

## 8. Validation and Exit Criteria

Required checks per phase:
- `npm run lint`
- `npm run build`
- Contract smoke tests against backend `/api/v1` envelope behavior.
- MSW parity checks for migrated endpoints.

Functional smoke paths:
- Login -> Dashboard data load.
- Signup -> Profile availability.
- Around-me: load threads, create/join/delete thread.
- Feed: load/create/like/comment post.
- Chat: room open, message load/send.
- Profile: me and member profile + dogs + diaries.

Definition of done for this architecture migration:
- Backend-contract API calls flow through one transport layer.
- Endpoint/method usage is constant-driven and traceable.
- UI state transitions are deterministic under typed errors.
- Legacy ad-hoc fetch paths are isolated or removed.
- Mock and runtime contract behavior remain aligned.
