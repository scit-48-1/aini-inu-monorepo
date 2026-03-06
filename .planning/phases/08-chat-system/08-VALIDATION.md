---
phase: 8
slug: chat-system
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-07
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | ESLint + TypeScript (`npm run build` type check) |
| **Config file** | eslint.config.mjs, tsconfig.json |
| **Quick run command** | `cd aini-inu-frontend && npm run lint` |
| **Full suite command** | `cd aini-inu-frontend && npm run build` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd aini-inu-frontend && npm run lint`
- **After every plan wave:** Run `cd aini-inu-frontend && npm run build`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 08-01-XX | 01 | 1 | CHAT-01, CHAT-02 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-01-XX | 01 | 1 | CHAT-03 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-02-XX | 02 | 1 | CHAT-04, CHAT-05 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-02-XX | 02 | 1 | CHAT-06, CHAT-12 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-02-XX | 02 | 1 | CHAT-07 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-02-XX | 02 | 1 | CHAT-13 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-03-XX | 03 | 2 | CHAT-08 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-03-XX | 03 | 2 | CHAT-09 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-03-XX | 03 | 2 | CHAT-10, CHAT-11 | type-check | `npm run build` | N/A | ⬜ pending |
| 08-03-XX | 03 | 2 | CHAT-14 | type-check | `npm run build` | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements.* No test runner to install — frontend uses lint + build only (per CLAUDE.md).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Room list renders loading/empty/error states | CHAT-01 | No test runner; visual UI states | Open /chat with no rooms, slow network, API error |
| WebSocket STOMP connects and receives events | CHAT-06, CHAT-12 | Requires live backend WS | Open chat room, send message from another user, verify real-time update |
| Polling fallback on WS disconnect | CHAT-13 | Network condition testing | Disconnect WS (devtools), verify 5s polling starts |
| Retry bubble appears on send failure | CHAT-07 | Requires simulated API error | Block /chat-rooms/{id}/messages, send message, verify retry bubble |
| Walk confirmation flow | CHAT-09 | Multi-user interaction | Two users in room, both confirm walk, verify allConfirmed |
| Review creation one-time guard | CHAT-10, CHAT-11 | Multi-step user flow | Submit review, reopen modal, verify button disabled |
| Cursor scroll preserves position | CHAT-04 | Visual scroll behavior | Scroll up to load older messages, verify no jump |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
