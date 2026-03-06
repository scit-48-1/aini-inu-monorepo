---
status: diagnosed
trigger: "Dashboard recent walk friends: (1) single item layout breaks, (2) clicking shows profile load error"
created: 2026-03-07T00:00:00Z
updated: 2026-03-07T00:00:00Z
---

## Current Focus

hypothesis: Two distinct bugs — CSS flex grow on single item + wrong ID passed for profile navigation
test: Code review complete
expecting: N/A
next_action: Return diagnosis

## Symptoms

expected: (1) Single item in recent friends list should display at same card size as when 5+ items. (2) Clicking a friend should navigate to their profile and load it successfully.
actual: (1) Single item stretches to fill container (layout break). (2) Clicking shows "failed to load profile" error.
errors: "프로필을 불러오는데 실패했습니다" on click
reproduction: Have only 1 chat room, view dashboard; click any recent friend item
started: After migration from old chatService to @/api/chat

## Eliminated

(none)

## Evidence

- timestamp: 2026-03-07
  checked: RecentFriends.tsx line 52 — flex container for friend cards
  found: Container uses `flex gap-6 overflow-x-auto` but cards only have `min-w-[160px]` with no `max-w-` or `flex-shrink-0`. When only 1 card exists, flexbox stretches it to fill available width.
  implication: Root cause of issue (1) — single item grows unbounded

- timestamp: 2026-03-07
  checked: dashboard/page.tsx lines 62-68 — friend ID mapping
  found: `id: String(r.chatRoomId)` — the friend's `id` is set to the chatRoomId, NOT a member/partner ID. ChatRoomSummaryResponse has no partner member ID field.
  implication: Root cause of issue (2) — navigating to `/profile/{chatRoomId}` tries to load a member by chatRoomId which is not a valid member ID

- timestamp: 2026-03-07
  checked: RecentFriends.tsx line 54 — navigation link
  found: `href={/profile/${friend.id}}` passes the friend.id (which is chatRoomId) to the profile route
  implication: Confirms wrong ID flows all the way to the profile page

- timestamp: 2026-03-07
  checked: profile/[memberId]/page.tsx line 19
  found: `OtherProfileView memberId={Number(memberId)}` — converts URL param to number, then calls getMember API with chatRoomId value instead of actual memberId
  implication: API call fails because no member exists with that ID (it's a room ID)

- timestamp: 2026-03-07
  checked: ChatRoomSummaryResponse in api/chat.ts
  found: Only has chatRoomId, chatType, status, lastMessage, updatedAt — NO partner/participant member ID
  implication: Dashboard cannot extract partner member ID from room summary. Needs either ChatRoomDetailResponse (has participants[].memberId) or a separate API call

## Resolution

root_cause: Two bugs — (1) flex cards lack `flex-shrink-0` / `max-w` constraint so single item stretches to fill container; (2) dashboard maps chatRoomId as friend.id instead of partner memberId, causing profile route to request a non-existent member
fix: (empty — diagnosis only)
verification: (empty)
files_changed: []
