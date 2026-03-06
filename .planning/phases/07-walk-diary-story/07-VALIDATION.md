---
phase: 7
slug: walk-diary-story
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-07
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | No frontend test runner (lint + build only per CLAUDE.md) |
| **Config file** | `aini-inu-frontend/next.config.ts`, `aini-inu-frontend/.eslintrc.json` |
| **Quick run command** | `cd aini-inu-frontend && npm run lint` |
| **Full suite command** | `cd aini-inu-frontend && npm run build` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd aini-inu-frontend && npm run lint`
- **After every plan wave:** Run `cd aini-inu-frontend && npm run build`
- **Before `/gsd:verify-work`:** Full build must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 07-01-xx | 01 | 1 | DIARY-01, DIARY-02, DIARY-04, DIARY-05 | build + lint | `npm run build` | N/A | ⬜ pending |
| 07-02-xx | 02 | 1 | DIARY-03, DIARY-06 | build + lint | `npm run build` | N/A | ⬜ pending |
| 07-03-xx | 03 | 2 | DIARY-07 | build + lint | `npm run build` | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No test framework to install (frontend validation is lint + build only).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Diary create with 300 char limit and default public | DIARY-01 | No frontend test runner; UI interaction required | 1. Open profile HISTORY tab 2. Click '+' 3. Enter >300 chars (verify truncation/counter) 4. Submit with default visibility 5. Verify diary appears in list |
| Diary list loads in HISTORY tab with pagination | DIARY-02 | Browser rendering + scroll pagination | 1. Open profile HISTORY tab 2. Verify diary cards render 3. Scroll to load more 4. Verify empty state when no diaries |
| Diary detail view in flipbook modal | DIARY-03 | Visual flipbook UI verification | 1. Click diary card in HISTORY tab 2. Verify DiaryBookModal opens with correct data 3. Verify photo gallery, title, content, date |
| Diary edit with pre-filled form | DIARY-04 | Form pre-population + PATCH semantics | 1. Open diary detail 2. Click edit 3. Verify fields pre-filled 4. Modify content 5. Save and verify changes |
| Diary delete with confirmation | DIARY-05 | Confirmation dialog UX flow | 1. Open diary detail 2. Click delete 3. Verify confirmation dialog 4. Confirm 5. Verify diary removed from list |
| Following diary feed via story viewer | DIARY-06 | Story viewer navigation UX | 1. Go to feed page 2. Click story icon 3. Verify following diaries load in flipbook 4. Verify auto-advance between members |
| Story icon row with member grouping and 24h expiry | DIARY-07 | Visual layout + server-side expiry | 1. Go to feed page 2. Verify story icon row at top 3. Verify one icon per member 4. Verify only follower content 5. Verify old stories not shown |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
