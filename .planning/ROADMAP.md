# Roadmap: Aini-inu Frontend Realignment

## Overview

Realign the entire aini-inu frontend to match the backend API contract (73 endpoints, OpenAPI/Swagger). The roadmap flows from stabilization (critical bugs) through infrastructure (common API layer), to domain-by-domain feature alignment (auth, member, pet, walk, diary, chat, community, lost-pet), and finishes with cross-domain composition (dashboard) and full integration verification (settings + UAT). Every phase produces a frontend that works correctly against the live backend for that domain's endpoints.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Critical Bugs** - Runtime error audit, classification, and fix for page crashes, infinite loops, and network errors
- [ ] **Phase 2: Common Infrastructure** - Centralized API layer, ApiResponse envelope, error handling, pagination, image upload, auth interceptor, state patterns
- [x] **Phase 3: Authentication** - 3-step signup flow (Account/Profile/Pet), login, token refresh, logout (completed 2026-03-05)
- [x] **Phase 4: Member Profile/Relations** - Profile view/edit, other member profiles, follow/unfollow, search, walk stats, personality types (completed 2026-03-06)
- [x] **Phase 5: Pet Management** - Pet CRUD, main pet selection, breed/personality/walk-style master data (completed 2026-03-06)
- [x] **Phase 6: Walk Threads** - Thread CRUD, apply/cancel, map exploration, hotspots, GPS, chat-type selection (completed 2026-03-06)
- [x] **Phase 7: Walk Diary + Story** - Diary CRUD, following feed, story list with 24h expiry (completed 2026-03-06)
- [ ] **Phase 8: Chat System** - Room list/detail, direct chat, messages, WebSocket STOMP, walk confirm, reviews
- [ ] **Phase 9: Community Feed** - Post CRUD, comments, likes with optimistic update, presigned image upload
- [ ] **Phase 10: Lost Pet** - Missing report, sighting report, AI analysis, matching, chat connect on approve
- [ ] **Phase 11: Dashboard** - Cross-domain composition with greeting, stats, recommendations, pending reviews
- [ ] **Phase 12: Settings + Integration UAT** - Theme toggle, logout, full Swagger/PRD/DEC verification, runtime error zero

## Phase Details

### Phase 1: Critical Bugs
**Goal**: The frontend loads and navigates without crashes, infinite loops, or network errors from mismatched API calls
**Depends on**: Nothing (first phase)
**Requirements**: BUG-01, BUG-02, BUG-03
**Success Criteria** (what must be TRUE):
  1. Every page in the app renders without a JavaScript runtime crash in the browser console
  2. No infinite re-render loops or infinite API polling detected on any page
  3. All existing API calls use correct URL paths, HTTP methods, and payload shapes matching the Swagger spec
  4. Browser network tab shows zero 4xx/5xx errors caused by frontend request malformation
**Plans**: 3 plans

Plans:
- [x] 01-01-PLAN.md — Infrastructure safety net: API proxy rewrites, MSW toggle fix, error boundaries for all routes
- [x] 01-02-PLAN.md — Per-page defensive patches (infinite polling, null guards) + API mismatch catalog for Phase 2

### Phase 2: Common Infrastructure
**Goal**: A shared API layer exists that all domain screens consume, with consistent envelope parsing, error handling, pagination, image upload, auth tokens, and UI state patterns
**Depends on**: Phase 1
**Requirements**: INFRA-01, INFRA-02, INFRA-03, INFRA-04, INFRA-05, INFRA-06, INFRA-07
**Success Criteria** (what must be TRUE):
  1. Every API call in the codebase goes through the centralized api/ service modules (no direct fetch/axios calls in components)
  2. All API responses are parsed through the ApiResponse<T> envelope and only `.data` is exposed to consuming code
  3. API errors display user-friendly messages via toast, mapped from backend errorCode values
  4. JWT Bearer token is automatically attached to authenticated requests, 401 triggers silent refresh, expired refresh triggers logout redirect
  5. Reusable components exist for all 5 UI states (default/loading/empty/error/success) and at least one page demonstrates their usage
**Plans**: 3 plans

Plans:
- [x] 02-01-PLAN.md — API client foundation: types, auth store, HTTP client with envelope unwrap, JWT interceptor, refresh queue, error toast
- [ ] 02-02-PLAN.md — Domain API modules (auth, members, pets, threads, diaries) — 40 typed endpoint functions
- [ ] 02-03-PLAN.md — Domain API modules (chat, lost pets, community, upload) — 33 typed endpoint functions + image upload utility

### Phase 3: Authentication
**Goal**: Users can sign up through the 3-step flow, log in, stay logged in via token refresh, and log out
**Depends on**: Phase 2
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05, AUTH-06
**Success Criteria** (what must be TRUE):
  1. User can log in with email/password and land on the authenticated dashboard
  2. User can complete 3-step signup (Account -> Profile -> Pet) with each step validating before enabling the next
  3. Form validation enforces email format, password strength (upper/lower/digit/special), and nickname 2-10 chars with inline error messages
  4. Session persists across browser refreshes via automatic token refresh, and expired sessions redirect to login
  5. User can log out from the app and their refresh token is invalidated on the backend
**Plans**: 3 plans

Plans:
- [ ] 03-01-PLAN.md — AuthProvider, login rewiring, store fixes, layout auth guard
- [ ] 03-02-PLAN.md — 3-step signup flow (Account + Profile + Pet) with validation and per-step API calls
- [ ] 03-03-PLAN.md — UAT gap closure: session refresh fix, login error display, sidebar logout button

### Phase 4: Member Profile/Relations
**Goal**: Users can view and edit their profile, browse other members, follow/unfollow, search members, and see walk activity stats
**Depends on**: Phase 3
**Requirements**: MEM-01, MEM-02, MEM-03, MEM-04, MEM-05, MEM-06, MEM-07, MEM-08, MEM-09, MEM-10, MEM-11, MEM-12, MEM-13
**Success Criteria** (what must be TRUE):
  1. User can view their own profile with all fields populated from MEM-ME-GET and edit any field with changes persisting via MEM-ME-PATCH
  2. User can view another member's profile (including their pet list) and see the correct follow state
  3. User can follow/unfollow another member with immediate UI update, and follower/following counts update accordingly
  4. User can search members by keyword and browse follower/following lists with pagination
  5. User can view their walk activity statistics (total walks, distance, etc.)
**Plans**: 3 plans

Plans:
- [ ] 04-01-PLAN.md — Own profile: page orchestrator, MyProfileView, ProfileHeader refactor to MemberResponse, ProfileEditModal rewrite, walk stats heatmap
- [ ] 04-02-PLAN.md — Other profile: OtherProfileView, useFollowToggle optimistic rewire, NeighborsModal paginated rewire
- [ ] 04-03-PLAN.md — Member search: MemberSearchModal with debounced search-as-you-type, sidebar search button

### Phase 5: Pet Management
**Goal**: Users can register, edit, and delete pets, set a main pet, and all form selects are populated from master data endpoints
**Depends on**: Phase 4
**Requirements**: PET-01, PET-02, PET-03, PET-04, PET-05, PET-06, PET-07, PET-08
**Success Criteria** (what must be TRUE):
  1. User can register a new pet with breed/personality/walk-style selected from master data dropdowns, birthDate as a single canonical date field, and name max 10 chars enforced
  2. User can edit and delete existing pets, with the pet list reflecting changes immediately
  3. User can designate a main pet and the selection persists across page loads
  4. Registration is blocked when the user already has 10 pets, with a clear message explaining the limit
**Plans**: 4 plans

Plans:
- [ ] 05-01-PLAN.md — Fix PetResponse type, create useMasterData hook + PetForm, rewire ProfileDogs from DogType to PetResponse
- [ ] 05-02-PLAN.md — Rewrite pet modals (register/detail) for PetResponse, add delete confirmation + main-switch, wire mutations in MyProfileView
- [ ] 05-03-PLAN.md — Gap closure: add empty-state block to ProfileDogs for zero-pet users
- [ ] 05-04-PLAN.md — UAT gap closure: fix PET_PROFILE->PET_PHOTO upload purpose, fix birthDate empty in edit mode

### Phase 6: Walk Threads
**Goal**: Users can create, browse, join, and manage walk threads with map-based exploration and proper GPS/chat-type/validation enforcement
**Depends on**: Phase 5
**Requirements**: WALK-01, WALK-02, WALK-03, WALK-04, WALK-05, WALK-06, WALK-07, WALK-08, WALK-09, WALK-10, WALK-11, WALK-12, WALK-13, WALK-14
**Success Criteria** (what must be TRUE):
  1. User with a registered pet can create a walk thread with title (max 30 chars), time, participating pet(s), intro (max 500 chars), and INDIVIDUAL/GROUP chat type -- users without pets see a block message
  2. User can browse threads in list and map view, with around-me page auto-acquiring GPS on entry (Seoul City Hall fallback on failure) and manual re-search only (no auto-polling)
  3. User can apply to a thread and get immediate entry, with capacity-exceeded instantly rejected and duplicate apply treated idempotently
  4. User can cancel their application, and thread owners can edit/delete their threads
  5. Hotspot markers are visible on the map and threads auto-expire after 60 minutes
**Plans**: 8 plans

Plans:
- [x] 06-01-PLAN.md — Core hook rewrite (useRadarLogic): GPS acquisition, dual data fetch, manual refresh, expiry timer, EMERGENCY tab disabled
- [x] 06-02-PLAN.md — RecruitForm rewrite: thread create/edit with chatType toggle, pet selection, location, validation, pet-owner block
- [x] 06-03-PLAN.md — Map + sidebar rewire: ThreadMapResponse markers, hotspot overlay, thread detail popup with apply/cancel, paginated sidebar
- [x] 06-04-PLAN.md — UAT gap closure (backend): duplicate key fix, T404 after delete, applied field in ThreadResponse, active thread endpoint, expire filter in getThreads
- [x] 06-05-PLAN.md — UAT gap closure (frontend): maxParticipants auto-set, map zoom, hotspot popup bubbling, apply state, active thread guard, date range filter
- [x] 06-06-PLAN.md — UAT gap closure (UX polish): walkDate in popup, delete confirmation visual, formatRemainingTime utility
- [ ] 06-07-PLAN.md — UAT retest gap closure: map date filter + loadMore pagination location fix
- [ ] 06-08-PLAN.md — UAT retest gap closure: '모집 완료' badge for full threads

### Phase 7: Walk Diary + Story
**Goal**: Users can write walk diaries (with content limits and default public visibility), browse following feed, and view time-limited stories
**Depends on**: Phase 6
**Requirements**: DIARY-01, DIARY-02, DIARY-03, DIARY-04, DIARY-05, DIARY-06, DIARY-07
**Success Criteria** (what must be TRUE):
  1. User can create a walk diary entry with content (max 300 chars, default public visibility) and view it in their diary list
  2. User can view diary details, edit content, and delete entries with the list reflecting changes
  3. User can browse a following feed showing diary entries from members they follow
  4. Story section shows one grouped icon per member, stories expire after 24 hours, and only follower content appears
**Plans**: 4 plans

Plans:
- [x] 07-01-PLAN.md — Rewire data hooks (useWalkDiaries, useDiaryForm), create DiaryCreateModal, rewire ProfileHistory and MyProfileView for diary CRUD
- [x] 07-02-PLAN.md — Rewire DiaryBookModal + DiaryPageRenderer for WalkDiaryResponse, rewire StoryArea + feed page for StoryGroupResponse story viewer
- [x] 07-03-PLAN.md — Gap closure: wire onDelete to DiaryBookModal in MyProfileView, add delete confirmation dialog
- [ ] 07-04-PLAN.md — UAT gap closure: fix photo upload purpose, visibility toggle, content button overlap, arrow-only flipbook navigation

### Phase 8: Chat System
**Goal**: Users can participate in real-time chat with message history, WebSocket live updates, walk confirmation, and post-walk reviews
**Depends on**: Phase 6
**Requirements**: CHAT-01, CHAT-02, CHAT-03, CHAT-04, CHAT-05, CHAT-06, CHAT-07, CHAT-08, CHAT-09, CHAT-10, CHAT-11, CHAT-12, CHAT-13, CHAT-14
**Success Criteria** (what must be TRUE):
  1. User can view their chat room list with loading/empty/error states, and open any room to see message history loaded cursor-based (newest first, scroll up for older)
  2. User can start a 1:1 direct chat that reuses an existing room if one exists, and group chats enforce 3-10 member capacity
  3. User can send messages (max 500 chars) and see them appear in real-time via WebSocket STOMP, with created/delivered/read status indicators
  4. Failed message sends show a retry bubble, and WebSocket disconnect falls back to 5-second polling
  5. User can confirm a walk, leave a chat room, write a one-time non-editable review per room/target, and view their own review history
**Plans**: 3 plans

Plans:
- [ ] 08-01-PLAN.md — Chat infrastructure: @stomp/stompjs install, useChatStore + useChatWebSocket hook, rewire ChatList + ChatStartModal to api/chat.ts
- [ ] 08-02-PLAN.md — Chat room detail: rewire ChatHeader/ChatInput/MessageList, cursor pagination, optimistic send with retry, WebSocket real-time, 500-char limit
- [ ] 08-03-PLAN.md — Walk confirm + leave room + review: walk confirm toggle, leave with dialog, WalkReviewModal rewire, old chatService cleanup

### Phase 9: Community Feed
**Goal**: Users can create, browse, and interact with community posts including comments, likes, and image uploads
**Depends on**: Phase 2
**Requirements**: FEED-01, FEED-02, FEED-03, FEED-04, FEED-05, FEED-06, FEED-07, FEED-08
**Success Criteria** (what must be TRUE):
  1. User can create a post with image (via presigned URL upload) and body text (both required), and see it in the feed list
  2. User can view post details, edit their own posts (content required), and delete their posts
  3. User can add, edit, and delete comments -- with comment deletion allowed for both comment author and post author
  4. User can like/unlike a post with optimistic UI update that rolls back on failure
**Plans**: 3 plans

Plans:
- [ ] 09-01: TBD
- [ ] 09-02: TBD

### Phase 10: Lost Pet
**Goal**: Users can report lost pets, submit sightings, run AI analysis to find matches, and connect with reporters via chat
**Depends on**: Phase 8
**Requirements**: LOST-01, LOST-02, LOST-03, LOST-04, LOST-05, LOST-06, LOST-07
**Success Criteria** (what must be TRUE):
  1. User can create a lost-pet report and submit a sighting report with image
  2. User can trigger AI analysis on a lost-pet report and view candidate matches from the session snapshot, with match order fixed on re-entry
  3. User can approve a match, which creates a direct chat room with the sighting reporter
  4. AI analysis failure (500 / L500_AI_ANALYZE_FAILED) shows a clear error message and does not create a session
  5. Around-me page includes a sighting/report tab with the full image analysis flow accessible
**Plans**: 3 plans

Plans:
- [ ] 10-01: TBD
- [ ] 10-02: TBD

### Phase 11: Dashboard
**Goal**: The home dashboard composes data from multiple domains into a single cohesive view with graceful partial-failure handling
**Depends on**: Phase 4, Phase 6, Phase 8
**Requirements**: DASH-01, DASH-02, DASH-03, DASH-04, DASH-05
**Success Criteria** (what must be TRUE):
  1. Dashboard displays greeting, manner score, and walk activity stats from member/walk-stats APIs
  2. Walk recommendation cards appear based on hotspot data, and latest neighborhood threads are summarized
  3. Pending review modal appears for rooms with unwritten reviews, with submit and failure retry working
  4. If any individual section's API call fails, only that section shows an error fallback -- other sections remain functional
**Plans**: 3 plans

Plans:
- [ ] 11-01: TBD
- [ ] 11-02: TBD

### Phase 12: Settings + Integration UAT
**Goal**: Settings screen works, and the entire frontend passes full integration verification against all 73 endpoints, 34 FR requirements, and 31 DEC policies with zero runtime errors
**Depends on**: Phase 1-11
**Requirements**: SET-01, SET-02, SET-03, SET-04, SET-05, SET-06, SET-07
**Success Criteria** (what must be TRUE):
  1. User can toggle between light and dark theme and the preference persists across sessions
  2. Logout from settings works with success confirmation, and failure shows retry option
  3. All 73 API endpoints match Swagger spec (URL, method, request/response shapes) with zero mismatches
  4. All 34 PRD FR requirements and 31 DEC policy lock values are verified as implemented in the frontend
  5. Zero runtime errors across all pages and flows, confirmed by agent-browser UAT with screenshot evidence
**Plans**: 3 plans

Plans:
- [ ] 12-01: TBD
- [ ] 12-02: TBD
- [ ] 12-03: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8 -> 9 -> 10 -> 11 -> 12

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Critical Bugs | 2/2 | Complete | 2026-03-06 |
| 2. Common Infrastructure | 1/3 | In progress | - |
| 3. Authentication | 3/3 | Complete   | 2026-03-06 |
| 4. Member Profile/Relations | 6/6 | Complete   | 2026-03-06 |
| 5. Pet Management | 3/4 | In progress | - |
| 6. Walk Threads | 6/8 | In Progress|  |
| 7. Walk Diary + Story | 4/4 | Complete   | 2026-03-06 |
| 8. Chat System | 1/3 | In Progress|  |
| 9. Community Feed | 0/2 | Not started | - |
| 10. Lost Pet | 0/2 | Not started | - |
| 11. Dashboard | 0/2 | Not started | - |
| 12. Settings + Integration UAT | 0/3 | Not started | - |

---
*Roadmap created: 2026-03-06*
*Last updated: 2026-03-07*
