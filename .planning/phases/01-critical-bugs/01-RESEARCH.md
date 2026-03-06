# Phase 1: Critical Bugs - Research

**Researched:** 2026-03-06
**Domain:** Frontend crash prevention, React error boundaries, defensive coding in Next.js 16 / React 19
**Confidence:** HIGH

## Summary

Phase 1 is a triage-and-stabilize pass over the aini-inu frontend. The codebase is a Next.js 16.1.6 / React 19.2.3 app with 11 page routes, all client components (`'use client'`), making API calls through a centralized `apiClient.ts` that talks to `/api/v1/*`. The frontend currently has **zero error boundaries**, **zero `error.tsx` files**, and **zero `loading.tsx` files** anywhere in the route tree. When any API call or rendering logic fails, the entire page crashes with an unhandled exception.

A critical architectural issue: the Next.js config has **no proxy/rewrites** to the Spring Boot backend. All API calls to `/api/v1/*` hit the Next.js dev server itself. MSW (Mock Service Worker) is the only thing intercepting these requests -- and it **always starts in development mode** regardless of env vars (the documented `NEXT_PUBLIC_ENABLE_MSW` flag is never referenced in code). Testing against the live backend requires either adding Next.js rewrites or modifying the `apiClient.ts` base URL to point to `http://localhost:8080`. Additionally, `useRadarLogic` has a 10-second polling interval that could cause infinite network request spam when the backend is unreachable.

**Primary recommendation:** Add Next.js `error.tsx` files at root and per-route level, create a reusable React error boundary component for intra-page sections, add API proxy configuration to `next.config.ts`, fix the MSW toggle to actually respect env vars, and audit each page for null/undefined access patterns that crash when real API data shapes differ from MSW mock shapes.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **Testing environment:** Test against live backend (Docker at localhost:8080), not MSW mocks. MSW disabled (`NEXT_PUBLIC_ENABLE_MSW=false`) during all bug hunting. Use TestAuthController to generate JWT tokens. Docker backend assumed always running.
- **Fix scope boundary:** Minimal defensive patches only: try-catch, error boundaries, null guards, infinite loop breaks. Do NOT fix API URL/method/payload mismatches -- those are Phase 2 (INFRA-01/02). Goal = pages don't crash, not that API calls succeed. Catalog all API mismatches found during audit as a deliverable for Phase 2.
- **Page audit order:** Priority by user flow importance, auth-first: Landing > Login > Signup > Dashboard > Around-me > Feed > Chat > Profile > Settings. Only actual JavaScript runtime crashes are in scope -- blank sections from missing API data are NOT Phase 1. Add React error boundaries proactively around major page sections as a safety net.

### Claude's Discretion
- Error boundary component design and granularity
- Specific try-catch placement strategy
- Bug catalog format and structure
- How to organize the audit pass (static analysis vs runtime testing vs both)

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| BUG-01 | Full frontend runtime error audit and classification | Static analysis of all 11 pages, hooks, and services identifies crash vectors. API proxy gap and MSW toggle issue are root causes of most runtime errors when testing against live backend. |
| BUG-02 | Critical runtime error immediate fixes (page crashes, infinite loops, render failures) | Error boundary patterns (Next.js `error.tsx` + custom `ErrorBoundary` component), null guard patterns, `useRadarLogic` polling loop fix, `useEffect` dependency array issues identified. |
| BUG-03 | Network error fixes from API call mismatches (wrong URL/method/payload) | Per CONTEXT.md, this is scoped to defensive wrapping (not fixing mismatches). API proxy setup needed to actually reach backend. Mismatch catalog is a Phase 2 deliverable. |
</phase_requirements>

## Standard Stack

### Core (Already in Use -- No New Additions)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Next.js | 16.1.6 | Framework | Already installed, provides `error.tsx` convention |
| React | 19.2.3 | UI library | Already installed, class component ErrorBoundary pattern still valid |
| TypeScript | 5.9.3 | Type safety | Already installed |
| sonner | 2.0.7 | Toast notifications | Already installed and used throughout |

### Supporting (No New Installs Needed)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| next-themes | 0.4.6 | Theme provider | Already present, no changes needed |
| zustand | 5.0.11 | State management | Already present, no changes needed |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom ErrorBoundary class | react-error-boundary npm package | Not needed -- Next.js `error.tsx` + minimal custom class covers all cases. Zero new dependencies for Phase 1. |

**Installation:**
```bash
# No new packages needed for Phase 1
```

## Architecture Patterns

### Recommended Error Boundary Structure
```
src/
  app/
    error.tsx              # Root-level error boundary (catches all route errors)
    global-error.tsx       # Root layout error boundary (must include <html>/<body>)
    layout.tsx             # Existing root layout (add try-catch to auth guard)
    dashboard/
      error.tsx            # Dashboard-specific error boundary
    around-me/
      error.tsx            # Around-me error boundary
    feed/
      error.tsx            # Feed error boundary
    chat/
      error.tsx            # Chat error boundary
      [id]/
        error.tsx          # Chat room error boundary
    profile/
      [memberId]/
        error.tsx          # Profile error boundary
    settings/
      error.tsx            # Settings error boundary
  components/
    common/
      ErrorBoundary.tsx    # Reusable section-level error boundary (React class component)
```

### Pattern 1: Next.js Route-Level Error Boundary (`error.tsx`)
**What:** A `'use client'` component that Next.js automatically wraps around route segments as a React Error Boundary
**When to use:** Every route directory, to prevent full-page crashes from bubbling up
**Example:**
```typescript
// Source: https://nextjs.org/docs/app/api-reference/file-conventions/error
'use client';

import { useEffect } from 'react';
import { Typography } from '@/components/ui/Typography';
import { Button } from '@/components/ui/Button';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[Route Error]', error);
  }, [error]);

  return (
    <div className="flex flex-col items-center justify-center h-full gap-6 p-10">
      <Typography variant="h2">Something went wrong</Typography>
      <Typography variant="body" className="text-zinc-400">
        {error.message || 'An unexpected error occurred'}
      </Typography>
      <Button variant="primary" onClick={reset}>Try again</Button>
    </div>
  );
}
```

### Pattern 2: Reusable Section Error Boundary (React Class Component)
**What:** A class component that catches render errors in child trees, showing a fallback UI per-section
**When to use:** Wrap major page sections (dashboard hero, radar map, feed list, chat messages) so one section crash doesn't take down the page
**Example:**
```typescript
// Source: React docs -- Error Boundaries
'use client';

import React, { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[Section Error]', error, errorInfo);
    this.props.onError?.(error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <div className="p-6 text-center text-zinc-400">
          <p className="text-sm font-bold">This section encountered an error</p>
        </div>
      );
    }
    return this.props.children;
  }
}
```

### Pattern 3: Safe Async Data Fetching with Defensive Guards
**What:** Wrap all `useEffect` data fetches with try-catch, null-coalesce API results
**When to use:** Every hook and page that fetches data
**Example:**
```typescript
// Defensive pattern for API calls
useEffect(() => {
  let cancelled = false;
  const fetchData = async () => {
    try {
      const data = await someService.getData();
      if (!cancelled) setData(data ?? defaultValue);
    } catch (e) {
      console.error('Fetch failed:', e);
      if (!cancelled) setData(defaultValue);
    }
  };
  fetchData();
  return () => { cancelled = true; };
}, []);
```

### Pattern 4: API Proxy via Next.js Rewrites
**What:** Route `/api/v1/*` requests to the Spring Boot backend at localhost:8080
**When to use:** Required for testing against the live backend (CONTEXT.md locked decision)
**Example:**
```typescript
// next.config.ts addition
const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: '/api/v1/:path*',
        destination: `${process.env.NEXT_PUBLIC_API_PROXY_TARGET || 'http://localhost:8080'}/api/v1/:path*`,
      },
    ];
  },
  // ... existing config
};
```

### Anti-Patterns to Avoid
- **Fixing API contract mismatches in Phase 1:** Per CONTEXT.md, only catalog them for Phase 2. Wrap defensively, don't fix URLs/payloads.
- **Adding SWR/react-query:** Phase 2 concern (INFRA scope). Phase 1 uses existing fetch patterns with better error handling.
- **Removing MSW entirely:** MSW is still needed for some development flows. Phase 1 adds the ability to toggle it off via env var (which is documented but not implemented).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Route-level error catching | Custom wrapper around every page | Next.js `error.tsx` convention | Framework-native, catches server and client errors, provides `reset()` |
| Root layout error catching | Nothing (current state) | Next.js `global-error.tsx` | Only way to catch root layout errors; must provide own `<html>`/`<body>` |
| API proxy to backend | Custom middleware or fetch URL changes | Next.js `rewrites()` in `next.config.ts` | Built-in, transparent, no code changes in service layer |

**Key insight:** Next.js already provides the `error.tsx` convention specifically designed for this problem. The codebase just never used it. Creating these files is the highest-value, lowest-risk fix available.

## Common Pitfalls

### Pitfall 1: MSW Blocking -- App Renders Nothing Without MSW
**What goes wrong:** The `MSWProvider` returns `null` (renders nothing) until MSW is ready. In development, MSW always initializes. But the env var toggle (`NEXT_PUBLIC_ENABLE_MSW`) was never implemented in code. If someone sets `NODE_ENV !== 'development'`, the provider passes through immediately, but then all `/api/v1/*` requests hit the Next.js server (no backend proxy), returning HTML instead of JSON.
**Why it happens:** The MSW provider checks `process.env.NODE_ENV === 'development'` but does NOT check `NEXT_PUBLIC_ENABLE_MSW`.
**How to avoid:** Add the env var check to `MSWProvider.tsx`. When MSW is disabled, the provider should pass through immediately (set `mswReady = true` without starting the worker). Also add Next.js rewrites so requests reach the backend.
**Warning signs:** Non-JSON response errors in console; blank white screen on load.

### Pitfall 2: Infinite Polling in useRadarLogic
**What goes wrong:** `useRadarLogic` sets up a 10-second polling interval (`setInterval(fetchData, 10000)`). The `fetchData` callback depends on `mapCenter` from Zustand. When `fetchData` is redefined (due to dependency changes), the `useEffect` re-runs, creating new intervals without always properly cleaning up the previous ones. Additionally, if the backend is unreachable, this creates continuous failed network requests every 10 seconds.
**Why it happens:** `fetchData` is in the dependency array of `useEffect`, and `fetchData` depends on `mapCenter` from Zustand store. Any `mapCenter` change recreates `fetchData`, re-triggers the effect, creates new intervals.
**How to avoid:** Phase 1 fix: ensure cleanup is robust (it currently IS cleaning up via `return () => timers.forEach(clearInterval)`), but add error counting to stop polling after N consecutive failures. The interval recreation on dep change is actually correct but may cause brief duplicate polling.
**Warning signs:** Network tab showing continuous failed requests every 10 seconds.

### Pitfall 3: Null Access on API Response Shape Differences
**What goes wrong:** MSW returns mock data with guaranteed shapes (e.g., `room.partner.nickname` always exists). Real backend may return different shapes, missing fields, or `null` values. Property access on `null`/`undefined` crashes React rendering.
**Why it happens:** The code was developed against MSW mocks, never tested against the real backend. No optional chaining on nested property access.
**How to avoid:** Add optional chaining (`?.`) on all nested property accesses from API data. Add default values for all destructured properties.
**Warning signs:** `TypeError: Cannot read property 'X' of null/undefined` in console.

### Pitfall 4: `error.tsx` Does NOT Catch Root Layout Errors
**What goes wrong:** The root `layout.tsx` contains an auth guard (`useEffect` that reads `localStorage` and redirects). If this throws, `error.tsx` at the same level won't catch it.
**Why it happens:** Next.js error boundaries wrap the `children` of a layout, not the layout itself. Errors in `layout.tsx` bubble to the parent (which for root layout means `global-error.tsx`).
**How to avoid:** Create `global-error.tsx` in `app/` to catch root layout errors. Also add try-catch inside the root layout's `useEffect`.
**Warning signs:** Complete white screen with error only visible in browser console.

### Pitfall 5: Missing PATCH Method in apiClient
**What goes wrong:** The backend uses PATCH for partial updates (standard REST), but `apiClient.ts` only exposes `get`, `post`, `put`, `delete` -- no `patch` method. Services like `memberService.updateMe` use `put` instead of `patch`.
**Why it happens:** apiClient was built for MSW which accepts PUT for everything.
**How to avoid:** Note in mismatch catalog for Phase 2. For Phase 1, this doesn't cause crashes, just 405 errors from the backend (which are caught by try-catch).
**Warning signs:** 405 Method Not Allowed responses.

### Pitfall 6: Chat Room Page Polling Without Error Handling
**What goes wrong:** `chat/[id]/page.tsx` has a 3-second polling interval for new messages. If the backend is unreachable or the room doesn't exist, this generates continuous failed requests and `console.error` spam.
**Why it happens:** The polling `setInterval` catches errors with `console.error` but continues polling indefinitely.
**How to avoid:** Add consecutive failure counting and stop/backoff after N failures. Clear interval on component unmount (already done). Consider adding the room existence check before starting the poll.
**Warning signs:** Console flooding with error messages every 3 seconds.

## Code Examples

### Next.js `global-error.tsx` (Root Layout Error Handler)
```typescript
// Source: https://nextjs.org/docs/app/api-reference/file-conventions/error#global-error
'use client';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="ko">
      <body className="flex items-center justify-center min-h-screen bg-[#FDFCF8]">
        <div className="text-center space-y-6 p-10">
          <h2 className="text-2xl font-black text-navy-900">Something went wrong</h2>
          <p className="text-zinc-400">{error.message}</p>
          <button
            onClick={reset}
            className="px-8 py-4 bg-amber-500 text-navy-900 rounded-2xl font-black"
          >
            Try again
          </button>
        </div>
      </body>
    </html>
  );
}
```

### MSW Provider Fix (Respect Env Var Toggle)
```typescript
// MSWProvider.tsx -- add env var check
'use client';

import { useEffect, useState } from 'react';

export function MSWProvider({ children }: { children: React.ReactNode }) {
  const [mswReady, setMswReady] = useState(false);

  useEffect(() => {
    const initMsw = async () => {
      if (
        typeof window !== 'undefined' &&
        process.env.NODE_ENV === 'development' &&
        process.env.NEXT_PUBLIC_ENABLE_MSW !== 'false' // NEW: respect env var
      ) {
        const { worker } = await import('../mocks/browser');
        await worker.start({ onUnhandledRequest: 'bypass' });
        await new Promise<void>(resolve => {
          if (navigator.serviceWorker.controller) resolve();
          else navigator.serviceWorker.addEventListener('controllerchange', () => resolve(), { once: true });
        });
      }
      setMswReady(true);
    };

    if (!mswReady) initMsw();
  }, [mswReady]);

  if (!mswReady) return null;
  return <>{children}</>;
}
```

### API Proxy Rewrites for Live Backend Testing
```typescript
// next.config.ts -- add rewrites for backend proxy
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const target = process.env.NEXT_PUBLIC_API_PROXY_TARGET || 'http://localhost:8080';
    return [
      {
        source: '/api/v1/:path*',
        destination: `${target}/api/v1/:path*`,
      },
    ];
  },
  images: {
    remotePatterns: [
      // ... existing patterns
    ],
  },
};

export default nextConfig;
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Custom class ErrorBoundary only | Next.js `error.tsx` + `global-error.tsx` + custom class | Next.js 13+ (App Router) | Route-level error boundaries are declarative, no custom wiring |
| `forwardRef` for refs | Direct ref prop (React 19) | React 19 (2024) | Simpler component APIs, project already uses React 19 |
| Manual proxy setup | Next.js `rewrites()` config | Stable since Next.js 9 | Built-in, zero runtime overhead |

**Deprecated/outdated:**
- Class component ErrorBoundary is still the ONLY way to catch render errors in React (functional equivalent does not exist). This is not deprecated, just the only option.

## Identified Bug Vectors (Audit Pre-Scan)

This section catalogs issues found during static analysis. Organized by page in audit priority order.

### 1. Infrastructure-Level Issues (All Pages)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| No `error.tsx` anywhere | Missing safety net | CRITICAL | Any unhandled error crashes entire page |
| No `global-error.tsx` | Missing safety net | CRITICAL | Root layout errors show white screen |
| No API proxy to backend | Config gap | CRITICAL | Without MSW, all API calls fail with HTML response |
| MSW env var toggle not implemented | Config gap | HIGH | Cannot disable MSW via `NEXT_PUBLIC_ENABLE_MSW` |
| `apiClient.ts` missing `patch()` method | API gap | MEDIUM | Backend uses PATCH; frontend only has PUT |

### 2. Landing Page (`/`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| No API calls | N/A | NONE | Static page, unlikely to crash |

### 3. Login Page (`/login`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| `authService.login` returns `any` | Weak typing | LOW | Error is caught, but login response shape may differ from expectation |
| `fetchProfile()` call after login | Network error | LOW | Already wrapped in `.catch(() => {})` -- safe |

### 4. Signup Page (`/signup`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| `managerData.dogs?.[0]` optional access | Null risk | LOW | Already has fallback |
| `DaumPostcode` component | External dep | LOW | Could fail to load but won't crash page |

### 5. Dashboard (`/dashboard`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| `if (!userProfile) return null` | Blank screen | MEDIUM | Returns nothing if profile fetch fails -- should show error state |
| `diaryRes.value` accessed as object with `.filter` | Type mismatch | MEDIUM | `Object.values(diaryRes.value).filter((d: any) => d?.isDraft)` -- if backend returns array instead of object, `.filter` would work but `Object.values` on array is redundant |
| `rooms.value` filter chain: `r?.partner` | Null access | LOW | Optional chaining already present |
| Multiple `Promise.allSettled` results checked | Good pattern | NONE | Already using `allSettled` -- robust |

### 6. Around-me (`/around-me`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| `useRadarLogic` 10s polling | Infinite requests | HIGH | Polls continuously even when backend is unreachable |
| `fetchData` in useEffect dependency array | Re-render risk | MEDIUM | `fetchData` depends on `mapCenter`, causing effect re-runs on location change |
| `visibleMarkers.filter((m: any) => m.author?.id)` | Type coercion | LOW | Safe due to optional chaining |
| `if (!mounted \|\| isLoading) return null` | Blank screen | MEDIUM | No loading indicator, no error state |
| `new Image()` in `optimizeImage` | Browser API | LOW | Safe -- only called on user action |

### 7. Feed (`/feed`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| `fetchPosts` depends on `currentLocation` | Re-render risk | LOW | `useCallback` with correct dep array |
| `followingDiaries` accessed as Record | Type risk | MEDIUM | If backend returns different shape, `Object.entries` could fail |
| `handleStoryClick` accesses `diaryEntries[0][0]` | Index access | LOW | Guarded by length check |

### 8. Chat List (`/chat`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| Static placeholder page | N/A | NONE | No API calls, no crash risk |

### 9. Chat Room (`/chat/[id]`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| 3-second message polling | Infinite requests | HIGH | Continuous polling with only `console.error` on failure |
| `rooms.find(r => r.id === roomId)` | Null risk | MEDIUM | If `getRooms()` fails, `rooms` is undefined -- will throw |
| `currentRoom.partner.id` access | Null chain | MEDIUM | No optional chaining; if partner is null, crash |
| `partnerDiaries` iteration with `Object.values` | Type risk | LOW | Wrapped in inner try-catch |

### 10. Profile (`/profile/[memberId]`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| `use(params)` React 19 API | Modern pattern | NONE | Correct usage of `use()` for async params |
| `ProfileView` -- large component with many API calls | Multiple crash vectors | MEDIUM | Uses `useWalkDiaries`, `useFollowToggle`, direct service calls |

### 11. Settings (`/settings`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| `logout()` reads localStorage directly | Minor risk | LOW | Already wrapped in try-catch in store |
| No API calls | N/A | NONE | All local state operations |

### 12. Root Layout (`layout.tsx`)
| Issue | Type | Severity | Notes |
|-------|------|----------|-------|
| Auth guard reads `localStorage` in `useEffect` | Parse error risk | MEDIUM | `JSON.parse(raw)` could throw on corrupted data -- already wrapped in try-catch |
| No error boundary wrapping `children` | Missing safety | HIGH | Any child route error crashes entire app |

## Mismatch Catalog (Phase 2 Deliverable Seed)

These are API mismatches identified during static analysis. NOT in scope for Phase 1 fixes, but cataloged per CONTEXT.md decision.

| Service | Method | Frontend URL | Likely Backend URL | Issue |
|---------|--------|-------------|-------------------|-------|
| memberService | `updateMe` | `PUT /members/me` | `PATCH /members/me` | HTTP method mismatch (PUT vs PATCH) |
| memberService | `updateDog` | `PUT /members/me/dogs/:id` | `PATCH /pets/:id` | Both URL path and method likely differ |
| memberService | `registerDog` | `POST /members/me/dogs` | `POST /pets` | URL path likely differs (pets domain) |
| memberService | `deleteDog` | `DELETE /members/me/dogs/:id` | `DELETE /pets/:id` | URL path likely differs |
| memberService | `getMyDogs` | `GET /members/me/dogs` | `GET /pets/me` or similar | URL path likely differs |
| memberService | `submitReview` | `POST /members/:id/reviews` | Unknown -- may be in chat domain | Domain routing likely differs |
| memberService | `getWalkStats` | `GET /members/me/stats/walk` | Unknown | May not exist or different path |
| authService | `login` | `POST /auth/login` | `POST /auth/login` | Response shape likely differs (backend returns JWT tokens, not user object) |
| geminiService | `analyzeDogImage` | `POST http://localhost:8080/api/v1/pets/analyze` | Hardcoded absolute URL | Bypasses apiClient; works but inconsistent |
| apiClient | missing method | N/A | N/A | No `patch()` method available |

**Note:** Full mismatch verification requires the OpenAPI spec (`common-docs/openapi/openapi.v1.json`) and live backend testing. This seed list is based on naming convention analysis only.

## Open Questions

1. **JWT Token Storage Location**
   - What we know: The root layout checks `localStorage['aini_inu_v6_db'].currentUserId` for auth (an MSW-era pattern). The real backend uses JWT tokens.
   - What's unclear: Where should JWT tokens be stored when testing against the live backend? The `apiClient` doesn't send any Authorization header.
   - Recommendation: Phase 1 scope is "pages don't crash" -- the auth guard should gracefully handle missing/invalid auth state rather than crash. The actual JWT integration is Phase 2/3 scope. For Phase 1, ensure the auth guard redirects to login without crashing when the localStorage format doesn't match expectations.

2. **Backend TestAuthController Token Format**
   - What we know: CONTEXT.md says to use TestAuthController to generate JWTs for testing.
   - What's unclear: How the token should be stored and attached to requests. The apiClient doesn't include Authorization headers.
   - Recommendation: For Phase 1 runtime testing, the implementer may need to temporarily add a hardcoded Bearer token header to apiClient, or use browser dev tools to add it. This is testing infrastructure, not a code change to commit.

3. **Next.js Rewrite vs apiClient URL Change**
   - What we know: Both approaches would route API calls to the backend.
   - What's unclear: Whether the user prefers rewrites (transparent) or apiClient change (explicit).
   - Recommendation: Use Next.js rewrites -- it's non-invasive, doesn't change any service code, and can be environment-controlled via `NEXT_PUBLIC_API_PROXY_TARGET`.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | No test runner configured for frontend (per CLAUDE.md) |
| Config file | None |
| Quick run command | `cd aini-inu-frontend && npm run lint` |
| Full suite command | `cd aini-inu-frontend && npm run build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| BUG-01 | All runtime errors audited and classified | manual + lint | `cd aini-inu-frontend && npm run lint` | N/A (lint exists) |
| BUG-02 | Error boundaries catch crashes, no infinite loops | build + manual | `cd aini-inu-frontend && npm run build` | N/A (build exists) |
| BUG-03 | API calls wrapped defensively | build + manual | `cd aini-inu-frontend && npm run build` | N/A (build exists) |

### Sampling Rate
- **Per task commit:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Per wave merge:** `cd aini-inu-frontend && npm run lint && npm run build`
- **Phase gate:** Build succeeds + manual browser audit shows zero crashes on all pages

### Wave 0 Gaps
- [ ] No frontend test framework -- validation is via `lint` + `build` + manual browser testing
- [ ] `agent-browser` skill available for automated page-load crash detection (recommended for verification)
- [ ] No automated runtime error detection -- consider `window.onerror` / `window.onunhandledrejection` logging for manual audit

## Sources

### Primary (HIGH confidence)
- Codebase static analysis: All 11 page files, 6 service files, 8 hooks, 2 stores, root layout
- [Next.js error.tsx docs](https://nextjs.org/docs/app/api-reference/file-conventions/error) - error boundary convention
- [Next.js global-error.tsx docs](https://nextjs.org/docs/app/getting-started/error-handling) - root layout error handling
- [Next.js rewrites docs](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites) - API proxy pattern

### Secondary (MEDIUM confidence)
- [Next.js 16 error handling guide](https://eastondev.com/blog/en/posts/dev/20260106-nextjs-error-boundary-guide/) - community best practices
- [Next.js error handling patterns](https://devanddeliver.com/blog/frontend/next-js-15-error-handling-best-practices-for-code-and-routes) - try-catch vs error boundary guidance

### Tertiary (LOW confidence)
- API mismatch catalog: Based on naming convention analysis only, not verified against OpenAPI spec or live backend

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already installed, no new dependencies
- Architecture (error boundaries): HIGH - Next.js `error.tsx` is well-documented, React class ErrorBoundary is the standard pattern
- Bug vectors audit: HIGH - Based on direct codebase reading, not speculation
- API mismatch catalog: LOW - Based on naming conventions, needs live backend verification
- Pitfalls: HIGH - Based on actual code patterns found in static analysis

**Research date:** 2026-03-06
**Valid until:** 2026-04-06 (stable -- no fast-moving dependencies)
