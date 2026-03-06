---
phase: 5
slug: pet-management
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-06
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | No test runner configured (frontend-only phase) |
| **Config file** | none |
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
| TBD | 01 | 1 | PET-01 | manual-only | `npm run lint && npm run build` | N/A | pending |
| TBD | 01 | 1 | PET-02 | manual-only | `npm run lint && npm run build` | N/A | pending |
| TBD | 01 | 1 | PET-03 | manual-only | `npm run lint && npm run build` | N/A | pending |
| TBD | 01 | 1 | PET-04 | manual-only | `npm run lint && npm run build` | N/A | pending |
| TBD | 01 | 1 | PET-05 | manual-only | `npm run lint && npm run build` | N/A | pending |
| TBD | 01 | 1 | PET-06 | manual-only | `npm run lint && npm run build` | N/A | pending |
| TBD | 01 | 1 | PET-07 | manual-only | `npm run lint && npm run build` | N/A | pending |
| TBD | 01 | 1 | PET-08 | manual-only | `npm run lint && npm run build` | N/A | pending |

*Status: pending · green · red · flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. Frontend has no test runner (as documented in CLAUDE.md). Validation relies on lint + build + manual UAT.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Pet registration form submits all fields correctly | PET-01 | No frontend test runner | Fill all fields, submit, verify pet appears in list |
| Pet edit form pre-populates and patches correctly | PET-02 | No frontend test runner | Edit a pet, verify changes persist after reload |
| Pet deletion with confirmation dialog | PET-03 | No frontend test runner | Delete pet, verify removed from list |
| Main pet switch updates UI immediately | PET-04 | No frontend test runner | Switch main pet, verify indicator moves |
| Breeds dropdown populated from API | PET-05 | No frontend test runner | Open form, verify breed dropdown has API data |
| Personalities multi-select from API | PET-06 | No frontend test runner | Open form, verify personality options from API |
| Walking styles multi-select from API | PET-07 | No frontend test runner | Open form, verify walking style options from API |
| Name 10-char limit enforced, 10-pet limit with message | PET-08 | No frontend test runner | Try >10 chars, try adding 11th pet |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
