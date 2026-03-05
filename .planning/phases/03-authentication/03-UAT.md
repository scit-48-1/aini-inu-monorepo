---
status: complete
phase: 03-authentication
source: [03-01-SUMMARY.md, 03-02-SUMMARY.md]
started: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:00:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Login Form — Email & Password Fields
expected: Navigate to /login. You see an email input and a password input with a submit button. Fields are functional — you can type in them.
result: pass

### 2. Login Inline Error Display
expected: Submit the login form with wrong credentials (or leave fields empty). An error message appears inline near the form fields (not just a toast notification).
result: issue
reported: "잘못된 비밀번호를 입력해도 어떠한 에러메시지도 나타나지 않는다. (올바른 로그인은 dashboard로 이동됨)"
severity: major

### 3. Session Restoration on Page Load
expected: Log in, then close/reopen the browser tab (or hard-refresh). You remain logged in — no forced redirect to /login. The app silently restores your session.
result: issue
reported: "새로고침하면 로그인이 풀린다."
severity: major

### 4. Auth Guard Redirect
expected: While logged out, navigate directly to a protected page (e.g., /dashboard). You are redirected to /login automatically.
result: pass

### 5. Logout Flow
expected: While logged in, trigger logout (any logout button/link). You are redirected to /login and your session is cleared (refreshing protected pages redirects again).
result: skipped
reason: 로그아웃 버튼이 UI에 없음

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
passed: 7
issues: 2
pending: 0
skipped: 1

## Gaps

- truth: "Session is restored on page refresh — user stays logged in without being redirected to /login"
  status: failed
  reason: "User reported: 새로고침하면 로그인이 풀린다."
  severity: major
  test: 3
  artifacts: []
  missing: []

- truth: "Wrong credentials on login form shows an inline error message near the fields"
  status: failed
  reason: "User reported: 잘못된 비밀번호를 입력해도 어떠한 에러메시지도 나타나지 않는다. (올바른 로그인은 dashboard로 이동됨)"
  severity: major
  test: 2
  artifacts: []
  missing: []
