---
phase: 9
slug: community-feed
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-07
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None (frontend has no test runner) |
| **Config file** | none |
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
| 09-01-xx | 01 | 1 | FEED-01, FEED-08 | manual + build | `npm run build` | N/A | pending |
| 09-01-xx | 01 | 1 | FEED-02 | manual + build | `npm run build` | N/A | pending |
| 09-01-xx | 01 | 1 | FEED-03 | manual + build | `npm run build` | N/A | pending |
| 09-02-xx | 02 | 2 | FEED-04, FEED-05 | manual + build | `npm run build` | N/A | pending |
| 09-02-xx | 02 | 2 | FEED-06 | manual + build | `npm run build` | N/A | pending |
| 09-02-xx | 02 | 2 | FEED-07 | manual + build | `npm run build` | N/A | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements.* Frontend has no test runner; validation is via `npm run lint` + `npm run build` (TypeScript type checking).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Post creation with image upload | FEED-01, FEED-08 | UI interaction + presigned URL flow | Create post with image, verify it appears in feed |
| Post list infinite scroll | FEED-02 | Scroll behavior | Scroll to bottom, verify next page loads |
| Post detail view | FEED-03 | UI rendering | Click post, verify detail modal shows with comments |
| Post edit/delete | FEED-04, FEED-05 | UI interaction | Edit content, verify persistence; delete and verify removal |
| Comment add/delete with permissions | FEED-06 | Permission branching | Add comment, delete own comment; verify post author can delete others' comments |
| Like optimistic toggle + rollback | FEED-07 | Async UI behavior | Like post, verify instant UI update; simulate failure, verify rollback |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
