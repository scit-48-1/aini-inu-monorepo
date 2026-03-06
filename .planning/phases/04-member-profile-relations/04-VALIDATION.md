---
phase: 4
slug: member-profile-relations
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-06
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | No test runner configured (frontend) |
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
| 04-01-01 | 01 | 1 | MEM-01 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-01-02 | 01 | 1 | MEM-02 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-01-03 | 01 | 1 | MEM-11 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-01-04 | 01 | 1 | MEM-12 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-02-01 | 02 | 1 | MEM-03 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-02-02 | 02 | 1 | MEM-04 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-02-03 | 02 | 1 | MEM-07, MEM-08 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-02-04 | 02 | 1 | MEM-05, MEM-06, MEM-13 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-03-01 | 03 | 2 | MEM-09 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |
| 04-03-02 | 03 | 2 | MEM-10 | lint+build | `npm run lint && npm run build` | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No test runner to set up (frontend has no test runner per CLAUDE.md). Validation is via lint + build + manual UAT.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Own profile loads with all fields | MEM-01 | No test runner | Navigate to /profile/me, verify all fields populated |
| Profile edit saves changes | MEM-02 | No test runner | Edit nickname, save, reload, verify persisted |
| Other member profile loads | MEM-03 | No test runner | Navigate to /profile/{id}, verify fields |
| Other member pet list shows | MEM-04 | No test runner | View other profile, check pet tab |
| Follower list paginates | MEM-05 | No test runner | Open followers modal, scroll for more |
| Following list paginates | MEM-06 | No test runner | Open following modal, scroll for more |
| Follow action optimistic | MEM-07 | No test runner | Click follow, verify immediate UI update |
| Unfollow with rollback | MEM-08 | No test runner | Click unfollow, verify revert on error |
| Walk stats heatmap | MEM-09 | No test runner | View own profile, check heatmap renders |
| Member search works | MEM-10 | No test runner | Open search modal, type query, verify results |
| Personality types in edit | MEM-11 | No test runner | Open edit modal, verify personality selector |
| 5-state UI coverage | MEM-12 | No test runner | Test loading/empty/error/default/success states |
| Follow counts public | MEM-13 | No test runner | View other profile, verify counts visible |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
