# Phase 3: Authentication - Context

**Gathered:** 2026-03-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Wire all authentication screens to the new `api/auth.ts` infrastructure from Phase 2. Implement 3-step signup (Account → Profile → Pet), email login, token refresh, and logout. All auth state managed via AuthProvider + useUserStore. No non-auth screens touched.

API calls in scope:
- `POST /api/v1/auth/login` (FR-AUTH-001)
- `POST /api/v1/members/signup` → `POST /api/v1/members/profile` → `POST /api/v1/pets` (FR-AUTH-002)
- `POST /api/v1/auth/refresh` (FR-AUTH-003)
- `POST /api/v1/auth/logout` (FR-AUTH-004)
- `POST /api/v1/test/auth/token` (DEC-031, dev convenience)

</domain>

<decisions>
## Implementation Decisions

### Signup step → API mapping
- Step order: Account → Profile → Pet → Complete (corrected from current Account → Pet → Profile)
- **Step 1 (Account)**: collects email + password + nickname → calls `POST /members/signup` on "Next" → receives accessToken/refreshToken → stores tokens silently
- **Step 2 (Profile)**: collects profileImageUrl (optional) + selfIntroduction (optional), nickname pre-filled from step 1 (editable) → calls `POST /members/profile` with JWT from step 1
- **Step 3 (Pet)**: collects pet info using birthDate canonical (DEC-003, no age input) → calls `POST /api/v1/pets` — has visible "나중에 등록하기" skip button (optional step)
- Step 4 (Complete): success screen, redirect to /dashboard

### Email verification — REMOVED
- Current AccountStep has send-code/verify-code UI calling `/auth/email/send` and `/auth/email/verify` — these endpoints do NOT exist in the backend
- Remove email verification flow entirely
- AccountStep validates email format (regex) only; duplicate email rejected by backend with M007 error at signup call

### Account step fields
- Nickname collected in Account step alongside email + password — all 3 go to `POST /members/signup` together
- `memberType` defaults to `PET_OWNER` (backend default if not specified)
- Password validation: 8+ chars, uppercase, lowercase, digit, special character — inline criteria badges (keep existing UX)
- `canGoNext`: isPasswordValid AND password === confirmPassword AND email format valid (remove isEmailVerified gate)

### Profile step fields
- Nickname: required, pre-filled from step 1, editable (2-10 chars, Korean/alphanumeric)
- Profile image upload: optional (via presigned URL, Phase 2 `api/upload.ts`)
- Self-introduction: optional (max 200 chars)
- Other fields (phone, age, gender, MBTI, personalityTypeIds): deferred to profile edit page (Phase 4)

### Pet step skip behavior
- "나중에 등록하기" skip button always visible regardless of memberType
- Skipping goes directly to Complete step — no pet created
- If user skips, memberType remains as set in step 1; pet creation deferred to Phase 5 (Pet Management)

### Auth state wiring
- AuthProvider (React Context) wraps the app — handles token lifecycle: store accessToken in memory, store refreshToken in localStorage, call refresh on 401, handle expired refresh → redirect /login
- useUserStore remains for profile data only (no token state) — logout() updated to call `api/auth.ts logout()` + clear tokens + clear profile
- AuthProvider wraps useUserStore: AuthProvider owns tokens, useUserStore owns profile
- login/page.tsx rewired from old `authService` to `api/auth.ts login()`
- After successful login: store tokens in AuthProvider, call `memberService.getMe()` to populate useUserStore, redirect to /dashboard

### Component structure (composition-patterns)
- `LoginForm` — isolated form component, imported by login/page.tsx
- `SignupAccountStep` — Account step (email + password + nickname, no email verification)
- `SignupProfileStep` — Profile step (nickname editable + optional image + optional intro)
- `SignupPetStep` — Pet step with skip button (birthDate canonical, DEC-003)
- Each step as separate variant component; SignupPage orchestrates step state

### Form validation (PRD §9.1 / §9.2)
- All required inputs: Next/Submit button disabled until conditions met (button-disabled + inline error — dual defense)
- Nickname: 2-10 chars, Korean/alphanumeric pattern `^[가-힣a-zA-Z0-9]+$`
- Password: strength criteria inline badges (existing UX pattern, keep)
- Email: format regex only (no verification call)
- Errors shown inline below each field, not just toast

### Error handling
- M007 (409 duplicate email): inline error below email field "이미 사용 중인 이메일입니다."
- M003 (409 duplicate nickname): inline error below nickname field
- C002 (400 validation): map to field-level error if identifiable, else toast
- All other API errors: toast (Phase 2 pattern)

### 5-state coverage (PRD §8.3)
- Each step shows loading state while API call is in-flight (button spinner)
- login/page.tsx: error state shown inline on failed login, success redirects
- No empty state for auth screens (not applicable)

### Claude's Discretion
- Exact AuthProvider implementation (createContext + useReducer vs useState)
- Token storage key names in localStorage
- Exact step transition animation (keep existing CSS animation pattern)
- Profile image upload UX in ProfileStep (can use simplified version, full presigned flow is Phase 2 utility)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `api/auth.ts`: `login()`, `logout()`, `refreshToken()` — wire these to UI (replaces old `authService`)
- `api/client.ts` (Phase 2): handles ApiResponse envelope, 401 retry queue — AuthProvider's token store feeds into this
- `components/signup/AccountStep.tsx`: reuse structure, remove email verification block, add nickname field
- `components/signup/PetStep.tsx`: reuse but fix birthDate (remove age field, DEC-003)
- `components/signup/ManagerStep.tsx`: rename to ProfileStep, narrow fields to nickname + image + intro
- `components/signup/SignupComplete.tsx`: reuse as-is
- `store/useUserStore.ts`: update `logout()` to call `api/auth.ts logout()` + clear tokens
- `app/login/page.tsx`: replace `authService.login` with `api/auth.ts login()`, add AuthProvider integration
- `app/signup/page.tsx`: fix step order ACCOUNT → PROFILE → PET → COMPLETE

### Established Patterns
- `'use client'` on all pages and form components
- Zustand store without persist for in-memory state (accessToken)
- Zustand with persist for localStorage state (refreshToken) — see useConfigStore pattern
- sonner toast for API errors (3s auto-dismiss, Korean messages as-is)
- Inline criteria badges for password validation (existing AccountStep UX — keep)
- `cn()` utility for conditional classNames

### Integration Points
- `next.config.ts`: `/api/v1` proxy already configured (Phase 1) — auth endpoints route through this
- `api/upload.ts` (Phase 2): optional profile image upload in ProfileStep — can use simplified version
- Phase 2 `api/client.ts`: AuthProvider must provide accessToken to the HTTP client (interceptor reads from AuthProvider context or Zustand)
- `useUserStore.fetchProfile()`: called after login to populate profile state

</code_context>

<specifics>
## Specific Ideas

- User provided full spec: Account → Profile → Pet step order with API mapping per step
- Composition patterns: LoginForm, SignupAccountStep, SignupProfileStep, SignupPetStep as separate variant components
- AuthProvider wraps useUserStore: tokens in AuthProvider context, profile data in useUserStore
- DEC-031: test token API (`POST /test/auth/token`) retained in api/auth.ts as `getTestToken()` (already there, no UI needed)

</specifics>

<deferred>
## Deferred Ideas

- None — discussion stayed within phase scope

</deferred>

---

*Phase: 03-authentication*
*Context gathered: 2026-03-06*
