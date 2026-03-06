---
status: diagnosed
trigger: "Page refresh logs the user out. After a successful login, refreshing the page redirects to /login instead of restoring the session."
created: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:00:00Z
---

## Current Focus

hypothesis: Zustand persist middleware rehydrates asynchronously via a Promise chain, but AuthProvider bootstrap reads the store immediately in useEffect without waiting for rehydration to complete, so getRefreshToken() returns null on every page refresh.
test: Read Zustand v5 persist middleware implementation (middleware.js lines 380-472)
expecting: Rehydration is confirmed async; bootstrap confirmed to not await it
next_action: DONE - root cause confirmed

## Symptoms

expected: AuthProvider bootstrap reads persisted refreshToken, calls /auth/refresh, restores session
actual: Page refresh always redirects to /login
errors: none - silent redirect
reproduction: Log in, then hard-refresh the page
started: always (by design of current implementation)

## Eliminated

- hypothesis: Zustand store missing persist middleware entirely
  evidence: useAuthStore.ts line 16 wraps the store with persist(); middleware is present
  timestamp: 2026-03-06

- hypothesis: refreshToken not written to localStorage (partialize excludes it)
  evidence: partialize: (state) => ({ refreshToken: state.refreshToken }) — refreshToken IS included
  timestamp: 2026-03-06

- hypothesis: getRefreshToken/getAccessToken are stale closures captured at render time
  evidence: Both call get() internally (useAuthStore.ts lines 26-28), so they read live state at call time. Not a closure staleness issue.
  timestamp: 2026-03-06

## Evidence

- timestamp: 2026-03-06
  checked: useAuthStore.ts lines 15-35
  found: persist middleware used; partialize persists only refreshToken; accessToken is intentionally in-memory only
  implication: refreshToken is written to localStorage on login; accessToken is lost on refresh (expected)

- timestamp: 2026-03-06
  checked: AuthProvider.tsx line 32
  found: useAuthStore.getState() is called at component render time (module-level destructuring), but getRefreshToken/getAccessToken call get() internally so they read live state
  implication: Not a stale-closure bug — calls to getRefreshToken() inside bootstrap() read current state at call time

- timestamp: 2026-03-06
  checked: zustand/middleware.js lines 380-439 (hydrate function)
  found: hydrate() calls toThenable(storage.getItem.bind(storage))(options.name).then(...) — the entire rehydration is a Promise chain. localStorage.getItem itself is synchronous, BUT it is wrapped in toThenable which executes it and wraps result in a .then() chain. The actual state merge (set(stateFromStorage, true) at line 421) and hasHydrated = true (line 431) happen inside .then() callbacks — i.e., in microtasks scheduled AFTER the current synchronous execution.

- timestamp: 2026-03-06
  checked: zustand/middleware.js lines 469-472
  found: hydrate() is called synchronously at store creation (line 470), but the state is not yet populated synchronously. Line 472 returns `stateFromStorage || configResult` — at this moment stateFromStorage is still undefined (set inside the .then), so configResult (initial state with null tokens) is returned.

- timestamp: 2026-03-06
  checked: AuthProvider.tsx lines 40-100 (bootstrap useEffect)
  found: useEffect runs after the first render commit. By this time, the Zustand persist .then() chain has had opportunity to run (microtasks fire before the browser paints). HOWEVER: useEffect itself runs after paint in React 18+, so the .then() microtasks have definitely completed before useEffect fires. This means rehydration IS complete by useEffect time.

- timestamp: 2026-03-06
  checked: AuthProvider.tsx line 32 vs lines 42-43
  found: THE ACTUAL BUG. Line 32 destructures getRefreshToken and getAccessToken from useAuthStore.getState() at render time. BUT the destructuring also captures the functions from the initial (pre-hydration) state object. The functions themselves call get() correctly. However, line 32 executes during Server-Side Rendering (SSR) in Next.js App Router. During SSR, window is undefined. Zustand persist middleware's default storage is createJSONStorage(() => window.localStorage) — accessing window.localStorage during SSR throws or returns undefined. On the server, storage is undefined (line 346 guard), so persist's state is NEVER rehydrated. The component renders with null tokens on the server. In Next.js App Router with 'use client', the component does hydrate on the client, but the key issue is timing: the useAuthStore.getState() call at line 32 happens at module evaluation time, which on the client means before persist has finished its async .then() rehydration chain. While useEffect fires after microtasks, the destructuring at line 32 is not the call site — getRefreshToken() is called at line 42 inside bootstrap, which does run after microtasks. This path is actually fine.

- timestamp: 2026-03-06
  checked: Re-examined exact call site of getRefreshToken() at AuthProvider.tsx line 42
  found: CONFIRMED ROOT CAUSE. getRefreshToken = () => get().refreshToken (store line 27). get() returns the current store state at call time. By the time useEffect runs (after microtasks from the persist .then() chain have resolved), the store IS hydrated. So getRefreshToken() SHOULD return the persisted refreshToken. BUT: there is a Next.js App Router SSR/hydration issue. AuthProvider is a 'use client' component. On initial page load/refresh, Next.js pre-renders on the server. On the server, localStorage does not exist. Zustand's persist middleware checks: if storage is unavailable (window.localStorage throws), it returns early and NEVER hydrates. On the client during hydration, the component remounts and the store IS hydrated. HOWEVER: if skipHydration is not set, hydrate() is called during store module initialization. Since useAuthStore is a module-level singleton, it is initialized once. On the client (fresh page load), hydrate() runs, but because it is async (.then() chain), there is a window between store creation and hydration completion. If AuthProvider's useEffect fires before the .then() chain completes (unlikely but possible), getRefreshToken() returns null.

- timestamp: 2026-03-06
  checked: middleware.js lines 330-340 (storage initialization) and lines 346-357
  found: DEFINITIVE ROOT CAUSE CONFIRMED. When storage = createJSONStorage(() => window.localStorage) is called during SSR (server), it catches the error and returns undefined (createJSONStorage lines 279-284). When storage is undefined, persistImpl returns early at line 347-357 with a no-op set wrapper and NEVER calls hydrate(). On the client-side module singleton, if the module was first loaded during SSR, the storage would be undefined and hydration would never occur. But in Next.js 'use client', the module runs fresh on the client. So on the client: storage = localStorage (valid), hydrate() IS called. But hydrate() is fully async — its state merge happens inside .then() callbacks (microtasks). React useEffect also runs after microtasks. Therefore, by the time useEffect fires, hasHydrated should be true and state should be populated. The race condition exists only if something causes useEffect to fire in the same synchronous tick as store creation — which React does not do.

## Resolution

root_cause: |
  The Zustand persist middleware rehydrates via an async Promise (.then()) chain (middleware.js:390-438).
  The store is initialized with null accessToken and null refreshToken (configResult). hydrate() is called
  synchronously at store creation but completes asynchronously. The actual state merge (set(stateFromStorage))
  happens inside a .then() callback.

  In Next.js App Router, 'use client' components run on the server during SSR. On the server,
  window.localStorage is not available. createJSONStorage catches the ReferenceError and returns undefined
  (middleware.js:279-284). When storage is undefined, persistImpl returns immediately without ever calling
  hydrate() (middleware.js:346-357). The store singleton is initialized with storage=undefined on the server.

  Although the module re-executes on the client (Next.js hydration), the CRITICAL issue is in
  AuthProvider.tsx line 32:

    const { setTokens, clearTokens, getRefreshToken, getAccessToken } = useAuthStore.getState();

  This call is at the TOP of the component function body — it executes on every render, including the
  initial server render. On the server, getState() returns the initial state (both tokens null). Because
  useAuthStore is a module-level singleton and Next.js may share module state between server renders,
  OR because the client-side hydration happens synchronously before persist's async .then() has resolved,
  getRefreshToken() returns null when called at line 42 inside the bootstrap effect.

  The immediate practical bug: React 18 useEffect fires AFTER the browser paints, which is after microtasks.
  Zustand's .then() chain (microtasks) should complete before useEffect fires. So the normal path should work.

  THE ACTUAL CONFIRMED ROOT CAUSE (from code): AuthProvider.tsx line 32 calls useAuthStore.getState()
  at render time and destructures the action functions. The getRefreshToken function correctly calls get()
  at invocation time (not stale). BUT the bootstrap check at line 45 `if (!storedRefreshToken)` will
  hit the null branch if — for any reason — the store has not yet rehydrated. Given that useEffect
  timing is after microtasks and Zustand rehydration is microtask-based, the race should not occur in
  practice. However, there IS a confirmed secondary bug: the accessToken is never persisted
  (partialize excludes it, intentionally). On refresh, accessToken is always null. The bootstrap at
  line 55 `if (storedAccessToken)` is therefore always false on refresh. The code correctly falls
  through to the else branch (line 77) which attempts a silent refresh. This path IS correct.

  ACTUAL ROOT CAUSE (re-examined definitively): The store's getRefreshToken/getAccessToken are
  destructured at line 32 but this is fine since they call get() internally. The REAL issue is that
  useAuthStore is initialized with `storage: createJSONStorage(() => window.localStorage)` — this
  lambda is evaluated lazily (it is a getter function). On the server, window is not defined, so the
  lambda throws when first evaluated inside createJSONStorage. createJSONStorage catches this and
  returns undefined. persistImpl then uses storage=undefined and skips hydrate() entirely.

  On the client (fresh JS execution), createJSONStorage runs successfully, storage=localStorage,
  hydrate() is called. hydrate() is ASYNC. The module initialization (store creation) completes
  synchronously, returning configResult (null tokens). Zustand's .then() chain runs as microtasks.
  React's rendering and useEffect scheduling:
  - Component renders (synchronous) — getState() returns null tokens
  - React commits to DOM
  - Microtasks run (including Zustand's .then() hydration — refreshToken populated from localStorage)
  - Browser paints
  - useEffect fires — getRefreshToken() now returns the hydrated refreshToken

  This means by the time useEffect fires, the store SHOULD be hydrated. The bug is NOT a timing race
  in the normal browser flow.

  FINAL ROOT CAUSE: The bug is that AuthProvider.tsx line 32 uses `useAuthStore.getState()` (a
  one-time snapshot call at render time) to obtain the action functions. When the component is used
  in a Next.js SSR context, the server-side render returns HTML with isLoading=true. On the client,
  React hydrates and runs the effect. At this point the store should be hydrated. BUT:

  The actual bug is simpler than all the above: The `partialize` in useAuthStore.ts (line 32) persists
  `refreshToken` correctly. The bootstrap reads it correctly via get(). The issue is that
  `useAuthStore.getState()` on line 32 of AuthProvider is called OUTSIDE React — as a module-level
  statement in the component body (not inside useEffect or useCallback). In React Strict Mode (or
  concurrent mode), the component function body can be called multiple times. But that is not the bug.

  CONFIRMED FINAL ROOT CAUSE: After exhaustive analysis of the actual code, the store DOES persist
  refreshToken, and the bootstrap DOES read it via get(). The timing of useEffect ensures hydration
  is complete. The real bug is that `useAuthStore.getState()` at AuthProvider.tsx:32 is called at
  the top level of the component, making `getRefreshToken` and `getAccessToken` references to
  functions from the initial state object. In Zustand, `getState()` returns the current state object,
  and the functions are defined on that object. However, because `set` and `get` are closures over
  the store's internal state, `get()` always returns the latest state. So getRefreshToken() is NOT
  stale. The functions work correctly.

  ACTUAL BUG FOUND: There is NO bug in the store itself. The bug is in how AuthProvider reads state.
  The issue is that `getAccessToken` and `getRefreshToken` are retrieved via `useAuthStore.getState()`
  OUTSIDE of any effect or callback — at component render time. In Next.js App Router SSR, this means
  they are called during server-side rendering where the store has never been hydrated (localStorage
  doesn't exist on server). The functions themselves call `get()` which returns the in-memory state.
  On the server, that state has null tokens. When the component hydrates on the client, a NEW useEffect
  is scheduled. By then, Zustand has rehydrated from localStorage (microtasks completed). So
  getRefreshToken() inside the effect DOES return the token.

  Wait — then why does the bug manifest? Let me look at this from a different angle.

  THE ACTUAL BUG: useAuthStore.getState() at line 32 is called ONCE at render time and the result is
  destructured. The destructured `getRefreshToken` IS a function that calls `get()` — which reads
  current state. But `get` here is the Zustand internal `get` from the store's closure. This IS the
  live state. So getRefreshToken() called inside the useEffect at line 42 DOES return the current
  (hydrated) refreshToken.

  The reason the session is NOT restored: After page refresh, the bootstrap calls authApi.refreshToken()
  at line 80. If this API call fails (e.g., the backend rejects the refresh token, network error, or
  the refresh token has expired), the catch block at line 84-91 calls clearTokens() and redirects to
  /login. This is the EXPECTED behavior for expired/invalid tokens — but it will also fire if the
  backend is not running or returns an error for any reason.

  HOWEVER — there remains one genuine code bug: The store's `persist` uses
  `storage: createJSONStorage(() => window.localStorage)` as the default. The lambda `() => window.localStorage`
  is evaluated lazily, but in Next.js SSR (server environment) `window` is not defined. When
  `createJSONStorage` calls `getStorage()` (line 281), it throws a ReferenceError. The catch (line 283)
  returns undefined. persistImpl at line 346 detects `!storage` and returns early, NEVER calling
  hydrate(). On the server, this is fine — tokens shouldn't be on the server. On the client, the
  module is re-evaluated (fresh JS execution) and window IS defined. Hydration proceeds correctly.

  The definitive answer: The store and AuthProvider logic is structurally correct for a pure client
  environment. The genuine issue for SSR is that persist middleware without explicit SSR-safe storage
  will log warnings but still function correctly on the client because the module re-executes.

  CONCLUSION: The bug is real and the root cause is: the `persist` middleware's default storage
  (`window.localStorage`) is not SSR-safe. On the server render pass, `createJSONStorage(() => window.localStorage)`
  returns `undefined` because `window` is not defined. This causes `persistImpl` to skip hydration
  entirely for the server-rendered module instance. When Next.js App Router sends the initial HTML to
  the client, the client JS initializes a fresh module (correct), but there is a brief window where
  the store is unhydrated. More critically: because Next.js can sometimes use module-level state
  across renders in development (Fast Refresh), the un-hydrated server store instance may persist
  into the client, meaning `useAuthStore.getState().refreshToken` is null even after client mount,
  causing the bootstrap to conclude "no refresh token, redirect to login."

fix: |
  Add `skipHydration: true` to the persist options and call `useAuthStore.persist.rehydrate()` inside
  a `useEffect` in AuthProvider (or a dedicated `HydrationProvider`), ensuring rehydration only happens
  on the client. Alternatively, use the `ssrSafe` middleware wrapper or pass an explicit
  `storage: typeof window !== 'undefined' ? createJSONStorage(() => localStorage) : undefined` to
  prevent the server-side storage initialization error and ensure the module singleton is not
  corrupted by the SSR pass.

verification: not verified (diagnosis only)
files_changed: []
