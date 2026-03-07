---
phase: 10
slug: lost-pet
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-08
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | None (frontend has no test runner per CLAUDE.md) |
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
| 10-01-xx | 01 | 1 | LOST-01 | build | `npm run build` | N/A | ⬜ pending |
| 10-01-xx | 01 | 1 | LOST-02 | build | `npm run build` | N/A | ⬜ pending |
| 10-02-xx | 02 | 1 | LOST-03 | build | `npm run build` | N/A | ⬜ pending |
| 10-02-xx | 02 | 1 | LOST-04 | build | `npm run build` | N/A | ⬜ pending |
| 10-02-xx | 02 | 1 | LOST-05 | build | `npm run build` | N/A | ⬜ pending |
| 10-02-xx | 02 | 1 | LOST-06 | build | `npm run build` | N/A | ⬜ pending |
| 10-03-xx | 03 | 2 | LOST-07 | build | `npm run build` | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. Frontend validation is lint + build only (no test runner configured per CLAUDE.md).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Lost pet report creation with image upload | LOST-01 | No frontend test runner; requires real presigned URL flow | Create report form, attach image, verify preview, submit |
| Sighting submission with image | LOST-02 | Requires file upload + API chain | Fill sighting form, attach photo, submit, verify toast |
| AI analysis auto-trigger after report | LOST-03 | Requires backend AI service running | Create report, observe loading overlay, verify candidates appear |
| Session re-entry with fixed order | LOST-04 | Requires existing analysis session | Re-open analyzed report card, verify candidates load in same order |
| Match approval -> chat navigation | LOST-05 | Requires chat service running | Approve candidate, verify redirect to /chat/{roomId} |
| Analysis failure error display | LOST-06 | Requires backend 500 simulation | Trigger analysis failure, verify error message, no session created |
| EMERGENCY tab sub-tabs and flow | LOST-07 | Visual layout + interaction flow | Navigate around-me EMERGENCY tab, verify sub-tabs, test full flow |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
