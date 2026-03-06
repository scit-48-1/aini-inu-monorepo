---
status: diagnosed
trigger: "Entering wrong credentials on the login form shows NO error message (no inline error, no toast). Correct credentials work fine (redirects to dashboard)."
created: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:00:00Z
---

## Current Focus

hypothesis: handle401 in apiClient intercepts the 401 from the login endpoint, attempts a token refresh (which fails), then calls window.location.href = '/login' — silently resetting the page instead of throwing an error the LoginForm can catch and display.
test: trace the 401 response path through client.ts handle401 and observe that the redirect fires before or instead of any visible error surfacing to LoginForm
expecting: confirmed — the page reloads to /login with no error state preserved, swallowing both the inline fieldError and the toast
next_action: return diagnosis to caller — root cause confirmed, no fix to apply (diagnose-only mode)

## Symptoms

expected: Inline red error box ("이메일 또는 비밀번호를 확인해 주세요.") OR a toast should appear when wrong credentials are submitted.
actual: Nothing visible happens. The form returns to its blank state silently.
errors: No JS console error seen by the user; the page appears to refresh.
reproduction: Enter an invalid email/password on /login and submit.
started: Always broken for wrong-credential flow.

## Eliminated

- hypothesis: apiClient swallows ApiError and never re-throws it
  evidence: client.ts lines 177-182 — ApiError IS re-thrown after the toast. The catch block in request() is not the silencing point.
  timestamp: 2026-03-06T00:00:00Z

- hypothesis: LoginForm catch block uses wrong instanceof check
  evidence: ApiError extends Error and sets this.name = 'ApiError'. The class is defined in the same module imported by LoginForm. instanceof works correctly here.
  timestamp: 2026-03-06T00:00:00Z

- hypothesis: The toast fires in apiClient but LoginForm catch never runs (error silently eaten somewhere in the call stack)
  evidence: AuthProvider.login() calls authApi.login() which calls apiClient.post() — no try/catch wrapping in auth.ts or in AuthProvider.login(). The error propagates cleanly... EXCEPT when handle401 intercepts.
  timestamp: 2026-03-06T00:00:00Z

## Evidence

- timestamp: 2026-03-06T00:00:00Z
  checked: client.ts lines 129-131
  found: When fetch returns 401, request() immediately returns handle401<T>(...) — it does NOT fall through to the JSON-parse / ApiError-throw path.
  implication: The ApiError constructed from the backend's 401 JSON body is NEVER created. The error path skips directly to the token-refresh logic.

- timestamp: 2026-03-06T00:00:00Z
  checked: client.ts lines 197-238 (handle401)
  found: handle401 calls refreshAccessToken(). For a login attempt the user has no valid refresh token (fresh visitor), so refreshAccessToken() throws. handle401's catch block (lines 225-237) then: (1) calls toast.error('세션이 만료되었습니다') and (2) calls window.location.href = '/login'.
  implication: The page fully reloads to /login. All React state — including the pending setState from LoginForm — is destroyed. The user sees a blank login form with no error. The "session expired" toast fires but is destroyed in the same reload.

- timestamp: 2026-03-06T00:00:00Z
  checked: handle401 catch block, line 236: throw error
  found: The error IS re-thrown after window.location.href is set, but window.location.href triggers an immediate navigation. The promise rejection propagates in a dead React tree. LoginForm's catch never executes in the new page context.
  implication: Re-throwing after a hard redirect is effectively a no-op from the UI's perspective.

- timestamp: 2026-03-06T00:00:00Z
  checked: auth.ts line 31 — login() uses default apiClient.post() (no suppressToast, no skipAuth)
  found: The login call goes through the standard request() path, which means the 401 interception in handle401 applies to it.
  implication: The login endpoint is not exempted from 401 token-refresh handling, even though a 401 on /auth/login means "wrong credentials", not "expired session".

## Resolution

root_cause: >
  client.ts's 401 handler (handle401, lines 197-238) treats every 401 response — including those from the login endpoint — as an expired-session event. When wrong credentials are submitted, the backend returns 401. Instead of parsing the error body and throwing an ApiError that LoginForm can catch, handle401 attempts a token refresh. That refresh fails (no valid refresh token exists). The catch block in handle401 then fires window.location.href = '/login', causing a hard full-page reload. This destroys all React state before LoginForm's catch block can run, silently resetting the form with no visible error. The "session expired" toast it fires is also destroyed in the reload.

fix: >
  The login API call must bypass handle401. The minimal fix is to pass { skipAuth: true } to the apiClient.post() call inside auth.ts login(). skipAuth prevents the Authorization header from being injected AND (critically) sets the skipAuth flag checked at line 129 of client.ts: `if (response.status === 401 && !skipAuth)` — so a 401 from /auth/login will fall through to the normal JSON-parse path, construct an ApiError from the backend's error body, show a toast via the existing ApiError branch, and re-throw — letting LoginForm's catch set fieldError as intended.

verification: not applied (diagnose-only mode)

files_changed: []
