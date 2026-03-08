---
status: resolved
trigger: "RecentFriends section: images missing, names show memberId, duplicates, key error"
created: 2026-03-08T00:00:00Z
updated: 2026-03-08T00:00:00Z
---

## Current Focus

hypothesis: All 4 bugs trace to fetchRecentFriends in page.tsx building friend objects incorrectly
test: Code review of data mapping at lines 171-186
expecting: Confirm key uses memberId (non-unique), name falls back to memberId, img is hardcoded, no dedup
next_action: return diagnosis

## Symptoms

expected: Dog images display, dog names shown, unique cards, no React key errors
actual: Images missing, memberId shown as name, duplicate cards, key error on '9004'
errors: "Encountered two children with the same key '9004'" at RecentFriends.tsx:54
reproduction: Load /dashboard with chat rooms where same partner appears in multiple rooms
started: Phase 11 dashboard rewire

## Eliminated

(none needed - root causes identified on first pass)

## Evidence

- timestamp: 2026-03-08T00:01:00Z
  checked: page.tsx fetchRecentFriends (lines 163-191)
  found: |
    1. id = String(partner.memberId) -- used as React key in RecentFriends line 54
    2. Same member in multiple chat rooms produces duplicate entries with same id
    3. name = petNames || `Member ${partner.memberId}` -- falls back to "Member {id}" not actual dog name
    4. img = '/AINIINU_ROGO_B.png' -- hardcoded logo, not pet/profile image
    5. No deduplication by memberId before setRecentFriends()
  implication: All 4 bugs originate from fetchRecentFriends data mapping

- timestamp: 2026-03-08T00:02:00Z
  checked: ChatParticipantResponse type (chat.ts lines 28-35)
  found: |
    pets array has { petId, name } but NO image URL field
    profileImageUrl exists on participant but is never used
    nickname exists on participant but is never used for display
  implication: Pet image data not available in API response type; profile image available but unused

- timestamp: 2026-03-08T00:03:00Z
  checked: RecentFriends.tsx line 54
  found: key={friend.id} where id = String(partner.memberId)
  implication: When same member appears in 2+ chat rooms, React gets duplicate keys

## Resolution

root_cause: |
  fetchRecentFriends in page.tsx has 4 defects:
  1. KEY: Uses partner.memberId as both `id` and React key -- not unique across multiple chat rooms with same partner
  2. NAME: Falls back to `Member ${memberId}` when pets array is empty; should use partner.nickname
  3. IMAGE: Hardcodes '/AINIINU_ROGO_B.png' instead of using partner.profileImageUrl or pet image
  4. DEDUP: No deduplication -- same partner in multiple rooms creates duplicate cards
fix: (not applied - diagnosis only)
verification: (not applicable)
files_changed: []
