---
phase: 1
slug: critical-bugs
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-06
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | No test runner configured for frontend (lint + build only) |
| **Config file** | `aini-inu-frontend/.eslintrc.json` (lint), `aini-inu-frontend/next.config.ts` (build) |
| **Quick run command** | `cd aini-inu-frontend && npm run lint` |
| **Full suite command** | `cd aini-inu-frontend && npm run lint && npm run build` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd aini-inu-frontend && npm run lint`
- **After every plan wave:** Run `cd aini-inu-frontend && npm run lint && npm run build`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | BUG-01 | lint + build | `cd aini-inu-frontend && npm run lint && npm run build` | N/A (lint exists) | ⬜ pending |
| 01-01-02 | 01 | 1 | BUG-02 | lint + build | `cd aini-inu-frontend && npm run lint && npm run build` | N/A (build exists) | ⬜ pending |
| 01-02-01 | 02 | 1 | BUG-02 | lint + build | `cd aini-inu-frontend && npm run lint && npm run build` | N/A (build exists) | ⬜ pending |
| 01-02-02 | 02 | 1 | BUG-03 | lint + build + manual | `cd aini-inu-frontend && npm run lint && npm run build` | N/A (build exists) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] No frontend test framework — validation is via `lint` + `build` + manual browser testing
- [ ] `agent-browser` skill available for automated page-load crash detection (recommended for verification)
- [ ] No automated runtime error detection — manual browser audit required

*Existing infrastructure (lint + build) covers compilation and static analysis. Runtime crash verification requires manual testing or agent-browser.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Pages render without JS crashes | BUG-01, BUG-02 | No runtime test framework; requires browser execution | Start dev server + Docker backend, navigate all 11 pages, check console for errors |
| No infinite polling loops | BUG-02 | Requires network tab observation over time | Open around-me and chat pages, watch network tab for 30s, verify no repeated failed requests |
| Error boundaries display fallback UI | BUG-02 | Visual verification needed | Temporarily throw in a component, verify error boundary catches and shows fallback |
| API mismatch catalog completeness | BUG-03 | Requires comparison against OpenAPI spec | Compare catalog entries against `common-docs/openapi/openapi.v1.json` |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
