---
status: diagnosed
trigger: "UAT Test 12: full threads hidden same as expired - need '모집 완료' badge for full threads"
created: 2026-03-06T00:00:00Z
updated: 2026-03-06T00:00:00Z
---

## Current Focus

hypothesis: Backend filters out ALL non-time-expired RECRUITING threads but has no concept of "full"; frontend has no "모집 완료" badge logic
test: Code reading of filter chain (backend getThreads + getMapThreads) and frontend badge rendering
expecting: Confirm that full threads are returned to frontend but frontend lacks distinct badge
next_action: Return diagnosis

## Symptoms

expected: Full threads (currentParticipants >= maxParticipants) should remain visible with a "모집 완료" badge
actual: Full threads are shown (they pass backend filter) but have no distinct badge; they look identical to open threads
errors: N/A - behavioral gap, not an error
reproduction: Fill a thread to max capacity, observe sidebar - no "모집 완료" indicator
started: Always - feature was never implemented

## Eliminated

- hypothesis: Backend filters out full threads entirely
  evidence: getThreads() line 123-127 only filters by isExpired(now) and location radius; isExpired() only checks time (startTime+60min) and status (EXPIRED/DELETED). currentParticipants is never checked in filter. Full threads ARE returned.
  timestamp: 2026-03-06

## Evidence

- timestamp: 2026-03-06
  checked: WalkThreadStatus enum
  found: Only 3 values exist: RECRUITING, EXPIRED, DELETED. No FULL or CLOSED status.
  implication: No way to distinguish full threads at the status level

- timestamp: 2026-03-06
  checked: WalkThread.isExpired() (entity line 210-215)
  found: Only checks time-based expiry (startTime + 60 min) and status == EXPIRED/DELETED. Does NOT check participant count.
  implication: "Expired" is purely time-based; capacity fullness is a separate concept not modeled

- timestamp: 2026-03-06
  checked: WalkThreadService.getThreads() (line 104-133)
  found: Queries by status=RECRUITING, then filters by isExpired(now) and location. No capacity filter. Full threads pass through.
  implication: Full threads ARE returned to the frontend - they are not hidden

- timestamp: 2026-03-06
  checked: WalkThreadService.getMapThreads() (line 135-170)
  found: Same pattern - filters by status=RECRUITING and isExpired(now). No capacity filter. Returns currentParticipants and maxParticipants per marker.
  implication: Map markers for full threads are also returned

- timestamp: 2026-03-06
  checked: ThreadSummaryResponse (DTO)
  found: Has currentParticipants and maxParticipants fields. Has status field (always "RECRUITING" for returned threads). No "isFull" flag.
  implication: Frontend receives the data needed to compute fullness but backend provides no explicit flag

- timestamp: 2026-03-06
  checked: RadarSidebar.tsx badge logic (lines 25-35, 159-174)
  found: Only two badge states exist - "만료됨" (red, when expired) or remaining time (amber). No "모집 완료" badge. The component receives currentParticipants/maxParticipants but never compares them for a full-capacity badge.
  implication: Frontend MISSING "모집 완료" badge logic entirely

- timestamp: 2026-03-06
  checked: RadarMapSection.tsx detail popup (lines 274-475)
  found: Shows currentParticipants/maxParticipants as text, shows "만료됨" badge if expired, but no "모집 완료" badge. Apply button is always shown for non-owner, non-applied users regardless of capacity (backend rejects with CAPACITY_FULL error).
  implication: Detail popup also lacks full-thread distinction. Apply button should be disabled for full threads.

## Resolution

root_cause: Two-part gap - (1) No semantic distinction between "time-expired" and "capacity-full" anywhere in the system. WalkThreadStatus only has RECRUITING/EXPIRED/DELETED. (2) Frontend sidebar and map detail popup have no badge or visual treatment for threads where currentParticipants >= maxParticipants.
fix: (empty - diagnosis only)
verification: (empty)
files_changed: []
