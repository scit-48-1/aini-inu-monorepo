# Phase 2: Common Infrastructure - Context

**Gathered:** 2026-03-06
**Status:** Ready for planning
**Source:** User-directed spec (discuss-phase + manual refinement)

<domain>
## Phase Boundary

Create `aini-inu-frontend/src/api/` folder with centralized API infrastructure. All 73 backend endpoints get typed function wrappers organized by domain. No UI/UX changes. No component rewiring (Phases 3-12 do that). Backend and common-docs are read-only.

</domain>

<decisions>
## Implementation Decisions

### 2-A. API Client Common Module (api/client.ts)
- Base URL: `/api/v1` (Next.js rewrites proxy to localhost:8080, configured in Phase 1)
- All responses auto-unwrap from `ApiResponse<T>` envelope: `{ success, status, data, errorCode, message }`
  - `success === true` → return `data`
  - `success === false` → throw error with `errorCode` + `message`
- Auth token interceptor: `Authorization: Bearer {token}` header auto-injected
- Refresh token auto-renewal: on 401 → call AUTH-REFRESH → retry original request
- Concurrent 401 handling: queue pending requests, single refresh, replay all queued
- Refresh token expired: clear auth state, toast "세션이 만료되었습니다", redirect to /login
- Access token in memory (Zustand), refresh token in localStorage
- PATCH method support (missing from current apiClient)

### 2-B. Domain API Function Files (skeletons from Swagger)
Source of truth: `common-docs/openapi/openapi.v1.json` (73 endpoints)
- `api/auth.ts` (3 endpoints: login, logout, refresh + test token)
- `api/members.ts` (13 endpoints)
- `api/pets.ts` (8 endpoints: CRUD + breeds, personalities, walking-styles)
- `api/threads.ts` (9 endpoints: CRUD + apply + map + hotspot)
- `api/diaries.ts` (6 endpoints: CRUD + following list)
- `api/chat.ts` (13 endpoints: rooms + messages + reviews + walk-confirm + leave + direct)
- `api/lostPets.ts` (7 endpoints: CRUD + analyze + match + sighting)
- `api/community.ts` (10 endpoints: posts CRUD + comments + likes + stories)
- `api/upload.ts` (3 endpoints: presigned-url POST, presigned-upload PUT, local GET)
- Parameter/return types extracted from Swagger schemas
- NO barrel file (index.ts) in api/ folder — direct imports only

### 2-C. Image Upload Utility (api/upload.ts)
PRD FR-COMMUNITY-003 presigned URL 2-step flow:
1. `POST /api/v1/images/presigned-url` → get token + uploadUrl
2. `PUT /api/v1/images/presigned-upload/{token}` → actual upload
3. `GET /api/v1/images/local` → image retrieval
Shared across: pet registration, post creation, lost pet reports

### 2-D. Pagination Common Handling
Two patterns from Swagger:
- SliceResponse (cursor): `{ content, nextCursor, hasMore }` (chat messages)
- SliceResponse (offset): `{ content, pageable, last, ... }` (other lists)
Abstract both into common hooks/utilities

### Error Message Presentation
- Toast-only for all API errors (sonner)
- Display backend's `ApiResponse.message` as-is (Korean)
- `message` null → generic fallback
- 3-second auto-dismiss
- Network/timeout errors: "다시 시도" retry action button in toast
- Business/validation errors: no retry button
- Success: green toast only

### Claude's Discretion
- Exact TypeScript type definitions structure (inline vs separate types file)
- Pagination hook naming and API design
- Error categorization logic (network vs business)
- Token storage key names
- File size limits / allowed formats (follow backend constraints)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `apiClient.ts` (`src/services/api/apiClient.ts`): Current client — will be REPLACED by new `api/client.ts`
- Domain services (`src/services/api/memberService.ts`, etc.): Will be REPLACED by new `api/*.ts` files
- `useUserStore.ts` (`src/store/useUserStore.ts`): Zustand store — needs JWT token state added
- `useConfigStore.ts` (`src/store/useConfigStore.ts`): Reference for persist middleware pattern
- `ErrorBoundary.tsx` (`src/components/common/ErrorBoundary.tsx`): Phase 1 addition — stays
- `cn()` utility, sonner toast: Stay as-is

### Established Patterns
- `'use client'` everywhere — maintain
- No barrel files for api/ (per user instruction)
- Zustand with persist for localStorage-backed state
- async-parallel: use `Promise.all()` for independent API calls

### Integration Points
- `next.config.ts`: rewrites() proxy `/api/v1` → `localhost:8080` (Phase 1)
- `common-docs/openapi/openapi.v1.json`: 73 endpoints — single source of truth for all function signatures
- Backend controllers/DTOs: cross-reference for type accuracy
- Phase 1 API mismatch catalog: fix list for endpoint alignment

### Constraints
- aini-inu-backend/, common-docs/ — READ ONLY, zero modifications
- No UI/UX changes — API layer only
- No Swagger-absent endpoints — 73 existing endpoints only
- Existing components NOT rewired in this phase (Phase 3-12 scope)

</code_context>

<specifics>
## Specific Ideas

- Phase 1 API mismatch catalog feeds directly into aligning function signatures
- Context7 for HTTP client (axios/fetch) latest API reference
- react-best-practices: async-parallel (Promise.all), no barrel imports, SWR dedup if applicable

</specifics>

<deferred>
## Deferred Ideas

- 5-state UI components (INFRA-07) — user excluded from this phase scope (API layer only)
- Component rewiring to use new api/ functions — Phases 3-12
- SWR or TanStack Query integration — evaluate during domain phases

</deferred>

---

*Phase: 02-common-infrastructure*
*Context gathered: 2026-03-06 (refined by user)*
