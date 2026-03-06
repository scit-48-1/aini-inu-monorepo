# API Mismatch Catalog -- Phase 1 Audit Output

**Generated:** 2026-03-06
**Purpose:** Input for Phase 2 (Common Infrastructure) planning
**Scope:** All API calls in `aini-inu-frontend/src/services/api/`

---

## Summary

- Total API calls audited: 29
- Mismatches found: 14
- By type:
  - URL path: 7
  - HTTP method: 2
  - Payload shape: 1
  - Response shape: 3
  - Missing endpoint on backend: 1
  - Missing method in apiClient: 1 (no `patch()`)

---

## Mismatches by Service

### chatService.ts

| Function | Frontend URL | Frontend Method | Backend URL (OpenAPI) | Backend Method | Mismatch Type | Notes |
|----------|-------------|-----------------|----------------------|----------------|---------------|-------|
| `getRooms` | `/chat/rooms` | GET | `/api/v1/chat-rooms` | GET | **URL path** | Frontend uses `/chat/rooms`, backend uses `/chat-rooms` |
| `getOrCreateRoom` | `/chat/rooms` | POST | `/api/v1/chat-rooms/direct` | POST | **URL path + payload** | Frontend POSTs to `/chat/rooms` with `{ partnerId }`. Backend expects `/chat-rooms/direct` with `ChatRoomDirectCreateRequest` schema (field name may differ from `partnerId`) |
| `getMessages` | `/chat/rooms/{roomId}/messages` | GET | `/api/v1/chat-rooms/{chatRoomId}/messages` | GET | **URL path** | Segment difference: `/chat/rooms/` vs `/chat-rooms/` |
| `sendMessage` | `/chat/rooms/{roomId}/messages` | POST | `/api/v1/chat-rooms/{chatRoomId}/messages` | POST | **URL path** | Same segment mismatch as above |

### memberService.ts

| Function | Frontend URL | Frontend Method | Backend URL (OpenAPI) | Backend Method | Mismatch Type | Notes |
|----------|-------------|-----------------|----------------------|----------------|---------------|-------|
| `getMe` | `/members/me` | GET | `/api/v1/members/me` | GET | None | Matches correctly |
| `getMemberProfile` | `/members/{id}` | GET | `/api/v1/members/{memberId}` | GET | None | Matches correctly |
| `updateMe` | `/members/me` | **PUT** | `/api/v1/members/me` | **PATCH** | **HTTP method** | Backend uses PATCH for partial profile update. Frontend uses PUT. apiClient has no `patch()` method. |
| `getMyDogs` | `/members/me/dogs` | GET | `/api/v1/pets` | GET | **URL path** | Backend uses `/pets` for current user's pets, not `/members/me/dogs` |
| `getMemberDogs` | `/members/{id}/dogs` | GET | `/api/v1/members/{memberId}/pets` | GET | **URL path** | Frontend uses `/members/{id}/dogs`, backend uses `/members/{memberId}/pets` |
| `registerDog` | `/members/me/dogs` | POST | `/api/v1/pets` | POST | **URL path** | Same `/members/me/dogs` vs `/pets` mismatch |
| `updateDog` | `/members/me/dogs/{id}` | PUT | `/api/v1/pets/{petId}` | PATCH | **URL path + HTTP method** | Frontend uses `/members/me/dogs/{id}` with PUT; backend uses `/pets/{petId}` with PATCH |
| `deleteDog` | `/members/me/dogs/{id}` | DELETE | `/api/v1/pets/{petId}` | DELETE | **URL path** | Frontend uses `/members/me/dogs/{id}`, backend uses `/pets/{petId}` |
| `searchMembers` | `/members?q=...` | GET | `/api/v1/members/search?q=...` | GET | **URL path** | Frontend uses `/members?q=`, backend uses `/members/search?q=` |
| `follow` | `/members/me/follow/{targetId}` | POST | `/api/v1/members/me/follows/{targetId}` | POST | **URL path** | `follow` vs `follows` (missing trailing `s`) |
| `unfollow` | `/members/me/follow/{targetId}` | DELETE | `/api/v1/members/me/follows/{targetId}` | DELETE | **URL path** | Same `follow` vs `follows` mismatch |
| `submitReview` | `/members/{partnerId}/reviews` | POST | `/api/v1/chat-rooms/{chatRoomId}/reviews` | POST | **URL path** | Reviews are submitted per chat room, not per member. Frontend uses `/members/{id}/reviews` which does not exist in OpenAPI |
| `getWalkStats` | `/members/me/stats/walk` | GET | `/api/v1/members/me/stats/walk` | GET | None | Matches correctly. **Note:** Backend returns `WalkStatsResponse` (object), frontend types as `number[]` — **response shape mismatch** |

### threadService.ts

| Function | Frontend URL | Frontend Method | Backend URL (OpenAPI) | Backend Method | Mismatch Type | Notes |
|----------|-------------|-----------------|----------------------|----------------|---------------|-------|
| `getThreads` | `/threads?lat=&lng=` | GET | `/api/v1/threads` (Slice, no lat/lng) or `/api/v1/threads/map?latitude=&longitude=` | GET | **URL path + query params** | Backend has two endpoints: `/threads` (paginated Slice, no geo params) and `/threads/map` (geo query with `latitude`/`longitude` params). Frontend calls `/threads?lat=&lng=` which matches neither. |
| `getFollowingDiaries` | `/walk-diaries/following` | GET | `/api/v1/walk-diaries/following` | GET | None | Matches correctly. **Note:** Backend returns paginated Slice, frontend expects `Record<string, WalkDiaryType>` — **response shape mismatch** |
| `getWalkDiaries` | `/walk-diaries?memberId=` | GET | `/api/v1/walk-diaries?memberId=` | GET | None | Matches. **Note:** Backend returns paginated Slice, frontend expects `Record<string, WalkDiaryType>` — **response shape mismatch** |
| `saveWalkDiary` | `/walk-diaries/{id}` | POST | `/api/v1/walk-diaries/{diaryId}` | PATCH | **HTTP method** | Frontend uses POST, backend uses PATCH for partial diary updates |
| `createThread` | `/threads` | POST | `/api/v1/threads` | POST | None | Matches. **Note:** Backend requires specific `ThreadCreateRequest` schema fields; frontend sends an ad-hoc object — **potential payload mismatch** |
| `joinThread` | `/threads/{id}/join` | POST | `/api/v1/threads/{threadId}/apply` | POST | **URL path** | Frontend uses `/threads/{id}/join`, backend uses `/threads/{threadId}/apply` |
| `unjoinThread` | `/threads/{id}/join` | DELETE | No matching endpoint found in OpenAPI | N/A | **Missing endpoint** | Backend has no DELETE on `/threads/{id}/apply`. Cancel-apply endpoint not in spec. |
| `getHotspots` | `/threads/hotspot?hours=` | GET | `/api/v1/threads/hotspot` | GET | None | Matches. **Note:** OpenAPI does not document an `hours` query param — likely ignored by backend |
| `updateThread` | `/threads/{id}` | PUT | `/api/v1/threads/{threadId}` | PATCH | **HTTP method** | Frontend uses PUT, backend uses PATCH |
| `deleteThread` | `/threads/{id}` | DELETE | `/api/v1/threads/{threadId}` | DELETE | None | Matches correctly |

### postService.ts

| Function | Frontend URL | Frontend Method | Backend URL (OpenAPI) | Backend Method | Mismatch Type | Notes |
|----------|-------------|-----------------|----------------------|----------------|---------------|-------|
| `getPosts` | `/posts?memberId=&location=` | GET | `/api/v1/posts` | GET | None (URL OK) | Backend supports `page/size/sort` params. Frontend sends `memberId` and `location` which are not documented in OpenAPI — **undocumented params** |
| `createPost` | `/posts` | POST | `/api/v1/posts` | POST | None | Matches |
| `updatePost` | `/posts/{id}` | PUT | `/api/v1/posts/{postId}` | PATCH | **HTTP method** | Frontend uses PUT, backend uses PATCH |
| `deletePost` | `/posts/{id}` | DELETE | `/api/v1/posts/{postId}` | DELETE | None | Matches |
| `likePost` | `/posts/{id}/like` | POST | `/api/v1/posts/{postId}/like` | POST | None | Matches |
| `addComment` | `/posts/{id}/comments` | POST | `/api/v1/posts/{postId}/comments` | POST | None | Matches |
| `getComments` | `/posts/{id}/comments` | GET | `/api/v1/posts/{postId}/comments` | GET | None | Matches |
| `getStories` | `/stories` | GET | `/api/v1/stories` | GET | None | Matches. Backend returns paginated Slice, frontend types as `any[]` |

### locationService.ts

| Function | Frontend URL | Frontend Method | Backend URL (OpenAPI) | Backend Method | Mismatch Type | Notes |
|----------|-------------|-----------------|----------------------|----------------|---------------|-------|
| `getCoordinates` | `https://nominatim.openstreetmap.org/...` | GET (direct fetch) | N/A | N/A | None | Uses external Nominatim API directly. Not a backend call. Hardcoded absolute URL is intentional here (no backend proxy). |

---

## Missing from apiClient

1. **No `patch()` method** — `apiClient` exports `get`, `post`, `put`, `delete` but NOT `patch`. Backend uses PATCH for: `PATCH /members/me`, `PATCH /pets/{petId}`, `PATCH /walk-diaries/{diaryId}`, `PATCH /threads/{threadId}`, `PATCH /posts/{postId}`. All five of these are currently called with wrong HTTP methods (`PUT` or `POST`).

2. **No authentication headers** — `apiClient.ts` sends no `Authorization: Bearer {token}` header. Every backend endpoint requires JWT. The frontend currently relies on MSW mock handlers that do not enforce auth. Real backend calls will return 401 for all requests.

---

## Hardcoded Absolute URLs

- `locationService.ts` line 17: `https://nominatim.openstreetmap.org/search?...` — this is intentional for the external geocoding API (cannot go through `/api/v1` proxy). No change needed.
- No other hardcoded absolute URLs found in `src/services/api/`.

---

## Recommendations for Phase 2

### Priority 1: Critical (will cause 100% failure on real backend)

1. **Add JWT auth to apiClient** — Without `Authorization` headers, every API call fails with 401. Phase 2 must implement token storage, retrieval, and injection into request headers (including refresh token flow).

2. **Add `patch()` method to apiClient** — 5 service functions use wrong HTTP methods. Adding `patch<T>()` is a one-line addition.

3. **Fix chat URL path** — `/chat/rooms` → `/chat-rooms` (affects getRooms, getMessages, sendMessage, getOrCreateRoom).

4. **Fix `getOrCreateRoom` URL** — `/chat/rooms` → `/chat-rooms/direct`.

### Priority 2: High (domain feature breakage)

5. **Fix dogs/pets URL mismatch** — `memberService.getMyDogs`, `getMemberDogs`, `registerDog`, `updateDog`, `deleteDog` all use `/members/me/dogs/` and `/members/{id}/dogs/` which do not exist. Backend uses `/pets` domain.

6. **Fix `follow`/`unfollow` URL** — `/members/me/follow/{id}` → `/members/me/follows/{id}`.

7. **Fix `searchMembers` URL** — `/members?q=` → `/members/search?q=`.

8. **Fix `joinThread` URL** — `/threads/{id}/join` → `/threads/{threadId}/apply` (with required `ThreadApplyRequest` body).

9. **Fix thread geo query** — `/threads?lat=&lng=` → `/threads/map?latitude=&longitude=` for map view, or drop geo params and use `/threads` for list view.

### Priority 3: Medium (wrong update semantics)

10. **Fix `saveWalkDiary`** — POST → PATCH.
11. **Fix `updateThread`** — PUT → PATCH.
12. **Fix `updatePost`** — PUT → PATCH.
13. **Fix `updateMe`** — PUT → PATCH.

### Priority 4: Low (response shape adaptation)

14. **Backend returns paginated Slice for diaries/threads/posts** — Frontend expects arrays or plain objects. Need unwrapping adapter in apiClient or per-service type handling.

15. **`getWalkStats` response shape** — Backend returns `WalkStatsResponse` object, frontend types as `number[]`.

16. **`submitReview` endpoint** — Backend accepts reviews on `/chat-rooms/{chatRoomId}/reviews`, not `/members/{partnerId}/reviews`. Requires knowing the chat room ID at review time.

### Grouping Suggestions for Phase 2 Planning

| Phase 2 Plan | Scope |
|---|---|
| 02-01: apiClient hardening | Add `patch()`, add JWT auth headers, add token refresh interceptor |
| 02-02: Chat service fix | Fix all `/chat/rooms` → `/chat-rooms/direct` URL mismatches |
| 02-03: Member + Pets service fix | Fix dogs/pets domain, follow endpoint, search endpoint |
| 02-04: Thread + Walk diary service fix | Fix join/apply, geo params, PATCH method, diary endpoints |
| 02-05: Community service fix | Fix post PATCH, review endpoint, response shape unwrapping |
