# Phase 3: Authentication - Research

**Researched:** 2026-03-06
**Domain:** Frontend authentication flow (login, 3-step signup, token lifecycle, logout)
**Confidence:** HIGH

## Summary

Phase 3 wires existing auth UI screens to the Phase 2 API infrastructure (`api/auth.ts`, `api/members.ts`, `api/pets.ts`, `api/client.ts`). The backend API contract is fully defined in OpenAPI and the Phase 2 API modules already match the contract. The existing signup/login components need significant restructuring: step order change (Account -> Pet -> Manager becomes Account -> Profile -> Pet), email verification removal, nickname addition to Account step, and ManagerStep replacement with a slimmed-down ProfileStep.

The core challenge is the AuthProvider + useAuthStore integration. Phase 2 already created `useAuthStore` (Zustand with persist, partializing refreshToken to localStorage) and `api/client.ts` already reads tokens from it and handles 401 refresh queuing. The main work is: (1) creating an AuthProvider React Context that wraps the app for auth state awareness, (2) rewiring login/signup pages from old `authService` (services/api/apiClient.ts) to new `api/auth.ts` + `api/members.ts` (api/client.ts), and (3) replacing the old localStorage-based auth guard in layout.tsx with token-based auth.

**Primary recommendation:** Build AuthProvider as a thin React Context wrapper around `useAuthStore` that provides `isAuthenticated`, `isLoading`, and auth actions. The token storage and HTTP interceptor logic already exists in `useAuthStore` + `api/client.ts` -- do not duplicate it.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Signup step order: Account -> Profile -> Pet -> Complete (not current Account -> Pet -> Manager)
- Step 1 (Account): email + password + nickname -> POST /members/signup -> receives tokens
- Step 2 (Profile): nickname (editable, pre-filled) + optional profileImageUrl + optional selfIntroduction -> POST /members/profile with JWT
- Step 3 (Pet): birthDate canonical (no age input) -> POST /pets -- has skip button ("later registration")
- Step 4 (Complete): success screen -> redirect /dashboard
- Email verification REMOVED entirely (no /auth/email/send or /auth/email/verify)
- Nickname collected in Account step (goes to signup payload)
- memberType defaults to PET_OWNER
- Password: 8+ chars, uppercase, lowercase, digit, special char -- inline criteria badges (keep existing UX)
- canGoNext: isPasswordValid AND password === confirmPassword AND email format valid (remove isEmailVerified gate)
- Profile step: nickname required (2-10 chars), profile image optional, selfIntroduction optional (max 200 chars)
- Other profile fields (phone, age, gender, MBTI, personalityTypeIds) deferred to Phase 4
- Pet step skip: always visible, skipping goes to Complete, no pet created
- AuthProvider wraps app: accessToken in memory, refreshToken in localStorage, 401 refresh, expired -> /login redirect
- useUserStore for profile data only (no token state)
- login/page.tsx: replace authService with api/auth.ts login()
- After login: store tokens -> call memberService.getMe() -> populate useUserStore -> redirect /dashboard
- Component structure: LoginForm, SignupAccountStep, SignupProfileStep, SignupPetStep as separate components
- Form validation: dual defense (button-disabled + inline error)
- Nickname: 2-10 chars, pattern ^[a-zA-Z0-9]+$
- Error handling: M007 (duplicate email) inline, M003 (duplicate nickname) inline, C002 (validation) field-level if identifiable else toast
- 5-state: loading spinner on buttons during API calls

### Claude's Discretion
- Exact AuthProvider implementation (createContext + useReducer vs useState)
- Token storage key names in localStorage
- Exact step transition animation (keep existing CSS animation pattern)
- Profile image upload UX in ProfileStep

### Deferred Ideas (OUT OF SCOPE)
- None
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| AUTH-01 | Email login (FR-AUTH-001) | `api/auth.ts login()` already exists; rewire LoginPage from old `authService` to new API; store tokens in `useAuthStore`; populate profile via `api/members.ts getMe()` |
| AUTH-02 | 3-step signup flow Account -> Profile -> Pet (FR-AUTH-002) | Fix step order in signup/page.tsx; refactor AccountStep (remove email verification, add nickname); replace ManagerStep with ProfileStep (narrow fields); fix PetStep (birthDate canonical, add skip); wire each step to correct API endpoint |
| AUTH-03 | Token refresh (FR-AUTH-003) | Already implemented in `api/client.ts` handle401 + refreshAccessToken + `api/auth.ts refreshToken()`; AuthProvider needs to bootstrap auth state on mount by checking stored refreshToken |
| AUTH-04 | Logout with refresh token revocation (FR-AUTH-004) | `api/auth.ts logout()` exists; wire useUserStore.logout() to call it with refreshToken from useAuthStore, then clearTokens + clearProfile |
| AUTH-05 | Signup form validation (PRD 9.2) | Password criteria badges (existing UX), email regex, nickname 2-10 chars ^[a-zA-Z0-9]+$ with inline errors |
| AUTH-06 | Step-by-step progression gating (PRD 8.3) | canGoNext computed per step; button disabled until conditions met; each step validates before enabling next |
</phase_requirements>

## Standard Stack

### Core (already installed/configured)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| React 19 | 19.x | UI framework | Project standard |
| Next.js 16 | 16.x | App Router, routing | Project standard |
| Zustand | 5.x | State management (useAuthStore, useUserStore) | Already in use, persist middleware for token storage |
| sonner | latest | Toast notifications | Project standard for error/success feedback |

### Supporting (already available)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| lucide-react | latest | Icons | Already used in all signup/login components |
| canvas-confetti | latest | Signup completion celebration | Already used in SignupComplete |
| api/client.ts | Phase 2 | HTTP client with auth interceptor | All API calls |
| api/auth.ts | Phase 2 | Auth API functions | login, logout, refreshToken, getTestToken |
| api/members.ts | Phase 2 | Member API functions | signup, createProfile, getMe |
| api/pets.ts | Phase 2 | Pet API functions | createPet |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| React Context AuthProvider | Zustand-only auth | Context provides component-tree awareness (isAuthenticated for conditional rendering); Zustand alone requires hook calls but already handles storage. Use thin Context wrapper. |

**Installation:** No new packages needed. All dependencies already installed.

## Architecture Patterns

### Recommended Project Structure
```
src/
  providers/
    AuthProvider.tsx          # NEW: React Context for auth state awareness
  api/
    auth.ts                   # EXISTS: login, logout, refreshToken (Phase 2)
    members.ts                # EXISTS: signup, createProfile, getMe (Phase 2)
    pets.ts                   # EXISTS: createPet (Phase 2)
    client.ts                 # EXISTS: HTTP client with 401 interceptor (Phase 2)
  store/
    useAuthStore.ts           # EXISTS: token storage (Zustand + persist)
    useUserStore.ts           # EXISTS: profile data (needs logout() update)
  components/
    auth/
      LoginForm.tsx           # NEW: extracted form from login/page.tsx
    signup/
      SignupAccountStep.tsx   # REWRITE: from AccountStep.tsx (remove email verify, add nickname)
      SignupProfileStep.tsx   # REWRITE: from ManagerStep.tsx (narrow to nickname + image + intro)
      SignupPetStep.tsx        # REWRITE: from PetStep.tsx (birthDate canonical, add skip)
      SignupComplete.tsx       # EXISTS: minor prop adjustment
  app/
    login/page.tsx            # MODIFY: use LoginForm + AuthProvider
    signup/page.tsx           # REWRITE: fix step order, new orchestration
    layout.tsx                # MODIFY: wrap with AuthProvider, replace localStorage guard
```

### Pattern 1: AuthProvider as Thin Context Wrapper
**What:** AuthProvider reads from `useAuthStore` and provides `isAuthenticated`, `isLoading`, `login()`, `logout()` actions via React Context. It does NOT duplicate token storage -- `useAuthStore` remains the single source of truth for tokens.
**When to use:** Wrap in layout.tsx so all pages can access auth state.
**Example:**
```typescript
// Source: Project pattern (composition of existing useAuthStore)
interface AuthContextValue {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { accessToken, refreshToken, setTokens, clearTokens } = useAuthStore();
  const { fetchProfile, logout: clearProfile } = useUserStore();
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  // Bootstrap: attempt token refresh on mount if refreshToken exists
  useEffect(() => {
    const bootstrap = async () => {
      if (refreshToken && !accessToken) {
        try {
          const result = await authApi.refreshToken({ refreshToken });
          setTokens(result.accessToken, result.refreshToken);
          await fetchProfile();
        } catch {
          clearTokens();
        }
      }
      setIsLoading(false);
    };
    bootstrap();
  }, []); // Run once on mount

  const loginFn = async (email: string, password: string) => {
    const result = await authApi.login({ email, password });
    setTokens(result.accessToken, result.refreshToken);
    await fetchProfile();
    router.push('/dashboard');
  };

  const logoutFn = async () => {
    if (refreshToken) {
      await authApi.logout({ refreshToken }).catch(() => {});
    }
    clearTokens();
    clearProfile();
    router.push('/login');
  };

  return (
    <AuthContext value={{ isAuthenticated: !!accessToken, isLoading, login: loginFn, logout: logoutFn }}>
      {children}
    </AuthContext>
  );
}

export function useAuth() {
  const ctx = use(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
```

### Pattern 2: Multi-Step Signup with Per-Step API Calls
**What:** Each signup step calls its own API endpoint on "Next", collecting tokens/state progressively. Step state managed locally in the orchestrating page component.
**When to use:** The signup page orchestrates step transitions and passes API results between steps.
**Example:**
```typescript
// Signup orchestration pattern
const [currentStep, setCurrentStep] = useState<'ACCOUNT' | 'PROFILE' | 'PET' | 'COMPLETE'>('ACCOUNT');

// Step 1 completes -> receives tokens -> silently stores them
const handleAccountComplete = (tokens: LoginResponse) => {
  useAuthStore.getState().setTokens(tokens.accessToken, tokens.refreshToken);
  setCurrentStep('PROFILE');
};

// Step 2 completes -> profile created with JWT auth
const handleProfileComplete = () => {
  setCurrentStep('PET');
};

// Step 3 completes (or skips) -> done
const handlePetComplete = () => {
  setCurrentStep('COMPLETE');
};
const handlePetSkip = () => {
  setCurrentStep('COMPLETE');
};
```

### Pattern 3: Inline Field Validation with Error Mapping
**What:** Form errors displayed inline below fields. API errors (M007, M003, C002) mapped to specific fields.
**When to use:** All auth forms.
**Example:**
```typescript
const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

try {
  await signup(data);
} catch (error) {
  if (error instanceof ApiError) {
    switch (error.errorCode) {
      case 'M007': // duplicate email
        setFieldErrors({ email: '이미 사용 중인 이메일입니다.' });
        break;
      case 'M003': // duplicate nickname
        setFieldErrors({ nickname: '이미 사용 중인 닉네임입니다.' });
        break;
      case 'C002': // validation error
        // attempt field mapping, fallback to toast
        toast.error(error.message);
        break;
      default:
        toast.error(error.message);
    }
  }
}
```

### Anti-Patterns to Avoid
- **Storing accessToken in localStorage:** Per CONTEXT.md decision, accessToken lives in memory only (Zustand without persist partialize). useAuthStore already handles this correctly -- `partialize: (state) => ({ refreshToken: state.refreshToken })`.
- **Duplicating token storage in AuthProvider state:** AuthProvider should READ from useAuthStore, not maintain its own copy. Single source of truth.
- **Using old authService/apiClient:** The old `services/authService.ts` and `services/api/apiClient.ts` use the pre-Phase-2 HTTP client without auth interceptor. All calls must go through `api/auth.ts` + `api/client.ts`.
- **Blocking password input behind email verification:** Email verification is removed. All fields show immediately.
- **Sending age field to backend:** PetCreateRequest requires birthDate, not age. DogFormFields has an age field that must be removed.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Token refresh on 401 | Custom interceptor | `api/client.ts` handle401 + refreshQueue | Already implemented in Phase 2 with queue-based concurrency handling |
| Token persistence | Custom localStorage code | `useAuthStore` with Zustand persist | Already partializes refreshToken to localStorage, accessToken in memory |
| API envelope parsing | Manual response.json() + .data | `api/client.ts` apiClient methods | Handles envelope unwrapping, error code extraction, toast |
| Email format validation | Complex regex | HTML5 `type="email"` + simple pattern | Browser native validation sufficient |
| Password strength check | External library | Existing inline criteria badges pattern | Already implemented in AccountStep, just reuse the pattern |

**Key insight:** Phase 2 built all the auth infrastructure. Phase 3 is purely UI wiring -- connect screens to existing APIs and stores. No new HTTP-level code needed.

## Common Pitfalls

### Pitfall 1: useAuthStore Hydration Mismatch
**What goes wrong:** Zustand persist rehydrates from localStorage async. On first render, `accessToken` is null even if refreshToken exists in localStorage, causing a flash of unauthenticated state.
**Why it happens:** SSR/initial render has no localStorage; hydration happens after mount.
**How to avoid:** AuthProvider must handle a `isLoading` state that starts `true`, attempts token refresh if refreshToken exists but accessToken is null, then sets `isLoading = false`. Protected pages show loading spinner until bootstrap completes.
**Warning signs:** User sees login page flash before being redirected to dashboard.

### Pitfall 2: Circular Import Between AuthProvider and api/client.ts
**What goes wrong:** If AuthProvider imports from `api/auth.ts` which imports from `api/client.ts` which imports from `useAuthStore`, and AuthProvider also imports `useAuthStore`, you get a potential circular dependency.
**Why it happens:** Token read path: client.ts -> useAuthStore. Token write path: AuthProvider -> useAuthStore.
**How to avoid:** This is already safe because `api/client.ts` uses `useAuthStore.getState()` (not a hook) to read tokens. AuthProvider can safely import both `useAuthStore` and `api/auth.ts`. No circular issue since they all converge on useAuthStore as the shared state.

### Pitfall 3: Old authService Still Used by Components
**What goes wrong:** Existing `login/page.tsx` imports `authService` from `services/authService.ts` which uses the OLD `services/api/apiClient.ts` (no auth header injection, no 401 handling). If not fully migrated, some calls bypass the new infrastructure.
**Why it happens:** Two API client layers exist: old (`services/api/apiClient.ts`) and new (`api/client.ts`).
**How to avoid:** Grep for all imports of `authService` and `services/authService` -- replace every one. After Phase 3, consider deleting `services/authService.ts` if no other consumers exist.
**Warning signs:** Login works but returns raw response without envelope unwrapping.

### Pitfall 4: Signup Step 1 Returns LoginResponse (Tokens)
**What goes wrong:** `POST /members/signup` returns `LoginResponse` with accessToken/refreshToken (verified from OpenAPI: response wraps LoginResponse). Developers might expect a MemberResponse.
**Why it happens:** Backend auto-logs-in the user on signup.
**How to avoid:** The `api/members.ts signup()` function currently returns `MemberResponse` -- this is WRONG per OpenAPI. It must return `LoginResponse`. The signup response matches the login response schema. Fix the return type.
**Warning signs:** Token extraction fails after signup.

### Pitfall 5: useUserStore.fetchProfile Guard Prevents Re-fetch
**What goes wrong:** `useUserStore.fetchProfile()` has a `hasFetched` guard that prevents re-fetching. After signup, if fetchProfile was already called (and failed because no token existed), subsequent calls are blocked.
**Why it happens:** The `if (get().hasFetched || get().isLoading) return;` guard.
**How to avoid:** Either reset `hasFetched` on new login/signup, or add a force parameter. The profile must be fetched fresh after every login/signup.

### Pitfall 6: Layout Auth Guard Uses Old localStorage DB_KEY
**What goes wrong:** `layout.tsx` checks `aini_inu_v6_db.currentUserId` in localStorage for auth. This is the MSW/mock-data pattern, not real auth.
**Why it happens:** Pre-Phase-2 auth relied on mock data store.
**How to avoid:** Replace the localStorage guard with AuthProvider's `isAuthenticated` state. Protected routes check `useAuth().isAuthenticated`.

## Code Examples

### Backend API Contract (verified from OpenAPI spec)

#### POST /members/signup
```typescript
// Request: MemberSignupRequest
{
  email: string;       // required, minLength: 1
  password: string;    // required, 8-64 chars, pattern: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).+$
  nickname: string;    // required, 2-10 chars, pattern: ^[a-zA-Z0-9]+$  -- NOTE: Korean chars included per PRD
  memberType?: string; // optional, defaults PET_OWNER. Enum: PET_OWNER, NON_PET_OWNER, ADMIN
}
// Response: ApiResponse<LoginResponse> -- NOT MemberResponse!
// Returns accessToken, refreshToken, memberId, etc.
```

#### POST /members/profile (requires JWT)
```typescript
// Request: MemberCreateRequest
{
  nickname: string;           // required, 2-10 chars
  profileImageUrl?: string;   // optional
  selfIntroduction?: string;  // optional, max 200 chars
  // All other fields optional, deferred to Phase 4
}
// Response: ApiResponse<MemberResponse>
```

#### POST /pets (requires JWT)
```typescript
// Request: PetCreateRequest
{
  name: string;        // required, 1-10 chars
  breedId: number;     // required
  birthDate: string;   // required, format: date (YYYY-MM-DD)
  gender: string;      // required, enum: MALE, FEMALE
  size: string;        // required, enum: SMALL, MEDIUM, LARGE
  isNeutered: boolean; // required
  mbti?: string;       // optional
  photoUrl?: string;   // optional
  isMain?: boolean;    // optional
  // walkingStyles, personalityIds optional
}
// Response: ApiResponse<PetResponse>
```

### Signup Return Type Fix
```typescript
// Source: OpenAPI spec -- POST /members/signup returns LoginResponse
// File: api/members.ts -- MUST FIX return type
import { LoginResponse } from './auth';

export async function signup(data: MemberSignupRequest): Promise<LoginResponse> {
  return apiClient.post<LoginResponse>('/members/signup', data);
}
```

### Auth Guard Replacement (layout.tsx)
```typescript
// Replace old localStorage guard with AuthProvider
// Old (remove):
// const DB_KEY = 'aini_inu_v6_db';
// useEffect checking localStorage...

// New: AuthProvider handles redirect in its bootstrap logic
// Protected pages use useAuth() hook
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `services/authService.ts` | `api/auth.ts` | Phase 2 | All auth API calls must use new module |
| `services/api/apiClient.ts` | `api/client.ts` | Phase 2 | New client has auth interceptor, 401 retry |
| localStorage `aini_inu_v6_db` auth check | `useAuthStore` token-based auth | Phase 2 | layout.tsx guard must be replaced |
| Email verification in AccountStep | Removed | CONTEXT.md decision | Remove send-code/verify-code UI |
| Step order: Account -> Pet -> Manager | Account -> Profile -> Pet | CONTEXT.md decision | Restructure signup page |
| Age input for pets | birthDate canonical only | DEC-003 | Remove age field from PetStep |

**Deprecated/outdated:**
- `services/authService.ts` -- old auth service using pre-Phase-2 apiClient; must not be used
- `services/api/apiClient.ts` -- old HTTP client without auth interceptor; being replaced by `api/client.ts`
- `useSignupForm.ts` hook -- sends all data in one giant payload; must be replaced with per-step API calls
- `DaumPostcode` / address search in ManagerStep -- not needed in ProfileStep (deferred)
- Email verification flow in AccountStep -- endpoints don't exist, removed by decision

## Open Questions

1. **Signup return type discrepancy**
   - What we know: OpenAPI says POST /members/signup returns LoginResponse (with tokens). Current `api/members.ts` declares return type as MemberResponse.
   - What's unclear: Whether the backend actually returns LoginResponse or MemberResponse in the response envelope.
   - Recommendation: Trust OpenAPI as source of truth (per CLAUDE.md priority). Fix the return type to LoginResponse.

2. **Nickname regex: Korean chars or not?**
   - What we know: OpenAPI pattern is `^[a-zA-Z0-9]+$` (no Korean). CONTEXT.md says "Korean/alphanumeric pattern `^[a-zA-Z0-9]+$`". But the description text in context says "Korean/alphanumeric".
   - What's unclear: The regex literally doesn't include Korean chars (`[가-힣]`).
   - Recommendation: Use the actual backend validation pattern from OpenAPI (`^[a-zA-Z0-9]+$`). If Korean is needed, it will be enforced by backend; for now match the spec. **UPDATE:** Re-reading CONTEXT.md more carefully: it says `^[가-힣a-zA-Z0-9]+$` explicitly. The OpenAPI spec shows `^[가-힣a-zA-Z0-9]+$` for MemberSignupRequest. Use the full pattern including Korean.

3. **useUserStore.fetchProfile hasFetched guard**
   - What we know: Guard prevents re-fetch after first call.
   - What's unclear: Whether resetting hasFetched or adding a force param is cleaner.
   - Recommendation: Add a `resetProfile()` action or modify `fetchProfile(force?: boolean)` to bypass guard when force=true. Call with force after login/signup.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | None (frontend has no test runner per CLAUDE.md) |
| Config file | none |
| Quick run command | `cd aini-inu-frontend && npm run lint && npm run build` |
| Full suite command | `cd aini-inu-frontend && npm run lint && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Login with email/password lands on dashboard | manual-only | N/A -- no test runner | N/A |
| AUTH-02 | 3-step signup (Account -> Profile -> Pet) with validation | manual-only | N/A | N/A |
| AUTH-03 | Token refresh persists session across browser refresh | manual-only | N/A | N/A |
| AUTH-04 | Logout revokes refresh token on backend | manual-only | N/A | N/A |
| AUTH-05 | Form validation (email, password strength, nickname) | manual-only | N/A | N/A |
| AUTH-06 | Step gating (next disabled until conditions met) | manual-only | N/A | N/A |

**Justification for manual-only:** Frontend has no test runner configured. Validation is via `npm run lint` (ESLint) and `npm run build` (TypeScript compilation). Functional testing requires browser interaction (agent-browser UAT).

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** Same as above
- **Phase gate:** lint + build green, then manual/agent-browser UAT

### Wave 0 Gaps
- None -- no test infrastructure to add (frontend has no test runner by design)

## Sources

### Primary (HIGH confidence)
- OpenAPI spec: `common-docs/openapi/openapi.v1.json` -- verified all auth endpoint schemas (AuthLoginRequest, MemberSignupRequest, MemberCreateRequest, PetCreateRequest, LoginResponse, TokenRefreshRequest, TokenRevokeRequest)
- Existing code: `api/auth.ts`, `api/client.ts`, `api/members.ts`, `api/pets.ts`, `store/useAuthStore.ts`, `store/useUserStore.ts` -- read in full
- Existing UI: `login/page.tsx`, `signup/page.tsx`, `AccountStep.tsx`, `ManagerStep.tsx`, `PetStep.tsx`, `SignupComplete.tsx` -- read in full
- CONTEXT.md: All locked decisions verified against OpenAPI contract

### Secondary (MEDIUM confidence)
- `useSignupForm.ts` hook -- current implementation analyzed for migration planning
- `services/authService.ts` -- old service analyzed to understand what must be replaced

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries already in use, no new dependencies
- Architecture: HIGH -- Phase 2 infrastructure fully analyzed, patterns verified against existing code
- Pitfalls: HIGH -- identified through direct code analysis (hydration, return types, guard patterns)

**Research date:** 2026-03-06
**Valid until:** 2026-04-06 (stable -- no external dependency changes expected)
