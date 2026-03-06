---
status: complete
phase: 03-authentication
source: [03-01-SUMMARY.md, 03-02-SUMMARY.md, 03-03-SUMMARY.md]
started: 2026-03-06T00:00:00Z
updated: 2026-03-06T02:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Login Form — Email & Password Fields
expected: Navigate to /login. You see an email input and a password input with a submit button. Fields are functional — you can type in them.
result: pass

### 2. Login Inline Error Display
expected: Submit the login form with wrong credentials (or leave fields empty). An error message appears inline near the form fields (not just a toast notification).
result: pass

### 3. Session Restoration on Page Load
expected: Log in, then close/reopen the browser tab (or hard-refresh). You remain logged in — no forced redirect to /login. The app silently restores your session.
result: pass
note: Fixed via 0680608 (SSR-safe custom storage) + 708fb54 (block children during bootstrap)

### 4. Auth Guard Redirect
expected: While logged out, navigate directly to a protected page (e.g., /dashboard). You are redirected to /login automatically.
result: pass

### 5. Logout Flow
expected: While logged in, trigger logout (any logout button/link). You are redirected to /login and your session is cleared (refreshing protected pages redirects again).
result: pass

### 6. Signup Account Step — Fields & Password Strength
expected: Navigate to /signup. Step 1 shows email, password, and nickname fields. As you type a password, you see 5 strength criteria badges that update in real-time (e.g., length, uppercase, number, special char checks).
result: pass

### 7. Signup Account Step — Duplicate Email/Nickname Errors
expected: On the Account step, submit with an email or nickname already in use. The specific error appears inline on that field (not just a generic toast) — e.g., "Email already registered" under the email field.
result: pass

### 8. Signup Profile Step
expected: After account creation, Step 2 shows a profile form with your nickname pre-filled. You can optionally upload a profile image (see a preview) and optionally add a self-introduction (up to 200 chars). Clicking Next advances without requiring the optional fields.
result: pass

### 9. Signup Pet Step — Skip Button
expected: Step 3 shows a pet registration form with birthDate input and a breed dropdown. A "Skip" button is always visible (not hidden or disabled). Clicking Skip bypasses pet registration and advances to the final step.
result: pass

### 10. Signup Complete — Auto-Redirect
expected: After completing or skipping the pet step, you see a completion screen (with confetti animation). After ~5 seconds, you are automatically redirected to /dashboard. You are now logged in.
result: pass

## Summary

total: 10
passed: 10
issues: 0
pending: 0
skipped: 0

## Gaps

- truth: "Session is restored on page refresh — user stays logged in without being redirected to /login"
  status: fixed
  reason: "User reported: 여전히 새로고침을 하니까 로그인이 풀려버려."
  severity: major
  test: 3
  root_cause: "Race condition: child components (DashboardPage) rendered and fired API calls before AuthProvider bootstrap completed. 401s triggered handle401 → refreshAccessToken() which consumed the refresh token via RTR. Bootstrap's own refresh arrived after the old token was deleted → 500 → clearTokens() → redirect. Secondary: Zustand v5 default storage evaluated localStorage at SSR module init time, but this was masked by the race condition."
  artifacts:
    - path: "aini-inu-frontend/src/providers/AuthProvider.tsx"
      issue: "Children rendered during bootstrap, causing competing refresh token calls with RTR"
    - path: "aini-inu-frontend/src/store/useAuthStore.ts"
      issue: "Default persist storage not SSR-safe"
  fix: "AuthProvider returns null while isLoading (blocks children until bootstrap completes). Custom storage with typeof window guard for SSR safety."
  fix_commits: ["0680608", "708fb54"]
