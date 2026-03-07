---
phase: 11
slug: dashboard
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-08
---

# Phase 11 — Validation Strategy

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
| TBD | 01 | 1 | DASH-01 | build | `npm run build` | n/a | ⬜ pending |
| TBD | 01 | 1 | DASH-05 | build | `npm run build` | n/a | ⬜ pending |
| TBD | 02 | 1 | DASH-02 | build | `npm run build` | n/a | ⬜ pending |
| TBD | 02 | 1 | DASH-03 | build | `npm run build` | n/a | ⬜ pending |
| TBD | 03 | 2 | DASH-04 | build | `npm run build` | n/a | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements.*

Frontend has no test runner configured. Validation is via `npm run lint` and `npm run build` per CLAUDE.md.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Greeting, manner score, walk stats display correctly | DASH-01 | UI rendering requires visual verification | Load dashboard, verify greeting text, manner gauge value, heatmap grid with correct cell counts |
| Hotspot recommendation card shows max-count region | DASH-02 | Data-driven UI display | Load dashboard, verify AI banner shows region with highest hotspot count |
| Latest threads show title/place/time without author | DASH-03 | Card layout change requires visual check | Load dashboard, verify thread cards show title, placeName, time range, participant count |
| Pending review modal opens on card click, submit/retry works | DASH-04 | Modal interaction flow | Create a chat room without review, load dashboard, click PendingReviewCard, verify modal opens with review form, submit, verify error retry |
| Individual section failure shows error fallback only for that section | DASH-05 | Partial failure requires network manipulation | Block one API endpoint (e.g., /walk-stats), verify only that section shows error fallback while others render normally |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
