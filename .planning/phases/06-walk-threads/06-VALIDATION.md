---
phase: 6
slug: walk-threads
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-06
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Next.js build + ESLint (no test runner configured) |
| **Config file** | `aini-inu-frontend/.eslintrc.json`, `aini-inu-frontend/next.config.ts` |
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
| 06-01-XX | 01 | 1 | WALK-01..05 | build+lint | `npm run build` | ✅ | ⬜ pending |
| 06-02-XX | 02 | 1 | WALK-06..10 | build+lint | `npm run build` | ✅ | ⬜ pending |
| 06-03-XX | 03 | 2 | WALK-11..14 | build+lint | `npm run build` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

*Existing infrastructure covers all phase requirements.*

Frontend has no test runner; validation relies on TypeScript compilation, ESLint, and production build checks. All requirements are UI-layer and verified through build + manual UAT.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| GPS acquisition on page entry | WALK-06 | Browser Geolocation API requires user permission | Navigate to /around-me, verify GPS prompt appears, coordinates update map center |
| Seoul City Hall fallback | WALK-07 | Requires simulating GPS failure | Deny GPS permission, verify map centers on 37.566295, 126.977945 |
| Map marker display | WALK-08 | Visual verification on Leaflet map | Browse threads in map view, verify markers appear at correct positions |
| Hotspot markers | WALK-10 | Visual verification on Leaflet map | Verify hotspot markers visible with region name + count popup |
| Thread expiry hiding | WALK-11 | Time-dependent behavior | Create thread, wait or mock time, verify expired threads hidden |
| Non-pet-owner block | WALK-01 | Requires user state variation | Login without pets, navigate to RECRUIT tab, verify block message |
| Apply flow toast + navigation | WALK-12 | User interaction flow | Apply to thread, verify toast with chat navigation button |
| Capacity exceeded rejection | WALK-13 | Requires full thread state | Fill thread to capacity, attempt apply, verify rejection toast |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
