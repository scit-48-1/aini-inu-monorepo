---
phase: 3
slug: authentication
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-06
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None (frontend has no test runner per CLAUDE.md) |
| **Config file** | none |
| **Quick run command** | `cd aini-inu-frontend && npm run lint && npm run build` |
| **Full suite command** | `cd aini-inu-frontend && npm run lint && npm run build` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd aini-inu-frontend && npm run lint && npm run build`
- **After every plan wave:** Run `cd aini-inu-frontend && npm run lint && npm run build`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-XX | 01 | 1 | AUTH-01 | lint+build | `npm run lint && npm run build` | N/A | pending |
| 03-01-XX | 01 | 1 | AUTH-03 | lint+build | `npm run lint && npm run build` | N/A | pending |
| 03-01-XX | 01 | 1 | AUTH-04 | lint+build | `npm run lint && npm run build` | N/A | pending |
| 03-02-XX | 02 | 2 | AUTH-02 | lint+build | `npm run lint && npm run build` | N/A | pending |
| 03-02-XX | 02 | 2 | AUTH-05 | lint+build | `npm run lint && npm run build` | N/A | pending |
| 03-02-XX | 02 | 2 | AUTH-06 | lint+build | `npm run lint && npm run build` | N/A | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No test runner to install (frontend validated via lint + build per CLAUDE.md).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Login with email/password lands on dashboard | AUTH-01 | No test runner; requires browser interaction | 1. Navigate to /login 2. Enter valid email/password 3. Click login 4. Verify redirect to /dashboard |
| 3-step signup completes with validation | AUTH-02 | Requires multi-step browser flow | 1. Navigate to /signup 2. Complete Account step 3. Complete Profile step 4. Complete/skip Pet step 5. Verify Complete screen |
| Token refresh persists session | AUTH-03 | Requires browser refresh during active session | 1. Login 2. Refresh browser 3. Verify still authenticated (no redirect to /login) |
| Logout revokes refresh token | AUTH-04 | Requires backend state verification | 1. Login 2. Click logout 3. Verify redirect to /login 4. Verify refreshToken cleared |
| Form validation enforces rules | AUTH-05 | Requires interactive form filling | 1. Test invalid email format 2. Test weak password 3. Test nickname < 2 or > 10 chars 4. Verify inline error messages |
| Step gating prevents progression | AUTH-06 | Requires interactive step navigation | 1. Leave required fields empty 2. Verify Next button disabled 3. Fill valid data 4. Verify Next button enabled |

---

## Validation Sign-Off

- [x] All tasks have automated verify (lint+build) or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
