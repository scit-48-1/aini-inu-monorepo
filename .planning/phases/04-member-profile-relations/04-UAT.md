---
status: complete
phase: 04-member-profile-relations
source: [04-01-SUMMARY.md, 04-02-SUMMARY.md, 04-03-SUMMARY.md, 04-04-SUMMARY.md]
started: 2026-03-06T02:00:00Z
updated: 2026-03-06T02:30:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Own Profile Page Load
expected: Navigate to your own profile page (/profile/[your-id] or click your avatar). The page shows your ProfileHeader with your name, avatar, follower count, and following count. No "undefined" or loading spinner stuck on screen.
result: issue
reported: "/profile/me도 실패, DB에서 확인한 본인 숫자 ID(/profile/[내 ID])로 접속해도 MyProfileView에서 '프로필을 불러오는데 실패했습니다.' 에러 상태와 다시시도 버튼이 표시됨. 다른 사람의 숫자 ID로는 정상 표시됨."
severity: major

### 2. Walk Heatmap
expected: On your own profile page, below your ProfileHeader, a GitHub-style heatmap grid of walk activity is visible. Each cell represents a day; days with walks show a colored square (darker = more walks). No errors or blank space where the heatmap should be.
result: skipped
reason: Blocked by Test 1 — MyProfileView fails to load for own profile

### 3. Profile Edit Modal
expected: Click the Edit button on your own profile. A modal opens with all profile fields: nickname, linked nickname, profile image URL, phone, age, gender (MALE/FEMALE/UNKNOWN), MBTI, self-introduction, and personality type chips (multi-select). Clicking a personality chip toggles it. Save submits and the profile updates.
result: skipped
reason: Blocked by Test 1 — MyProfileView fails to load for own profile

### 4. Other Member Profile
expected: Navigate to another member's profile (/profile/[other-id]). The page shows their ProfileHeader with their name, avatar, and a Follow button (or Unfollow if already following). Their registered dogs appear in the DOGS tab.
result: pass

### 5. Follow / Unfollow Toggle
expected: On another member's profile, click Follow. The button switches to Unfollow immediately (optimistic update) and the follower count increments. Click Unfollow — button reverts to Follow immediately and count decrements. No page reload required.
result: issue
reported: "팔로우 후 UI는 즉시 반응하지만, 새로고침하면 팔로우 상태가 사라짐. 백엔드에 실제로 저장되지 않거나 팔로우 상태 재조회가 잘못된 것으로 보임."
severity: major

### 6. Neighbors Modal (Followers / Following List)
expected: Click the follower or following count on any profile. A modal opens listing the members with their avatars and nicknames. If there are more than the initial batch, a "Load More" button appears and appends additional results when clicked.
result: issue
reported: "모달은 열림. 그러나 상대방을 팔로우한 후 상대방의 모달을 열면, 나(팔로워)가 팔로워 탭이 아닌 팔로잉 탭에 표시됨. 또한 팔로잉 카운트가 1 증가함 (팔로워 카운트가 증가해야 함)."
severity: major

### 7. Member Search from Sidebar
expected: Click the Search button in the sidebar (desktop: icon above the "+" button; mobile: Search item in the bottom nav). A search modal opens with an auto-focused input. Type a member name — results appear within ~300ms with avatar, nickname, and manner temperature badge. Clicking a result navigates to their profile and closes the modal.
result: pass

### 8. Own Profile DOGS Tab
expected: On your own profile, click the DOGS tab. Your registered pets appear with their names and photos. The list does NOT show an error or empty state if you have registered pets (i.e., it calls GET /pets, not a broken /members/0/pets endpoint).
result: skipped
reason: Blocked by Test 1 — MyProfileView fails to load for own profile

## Summary

total: 8
passed: 2
issues: 3
pending: 0
skipped: 3

## Gaps

- truth: "Own profile page loads when navigating to own profile"
  status: failed
  reason: "User reported: /profile/me도 실패, DB에서 확인한 본인 숫자 ID(/profile/[내 ID])로 접속해도 MyProfileView에서 '프로필을 불러오는데 실패했습니다.' 에러 상태와 다시시도 버튼이 표시됨. 다른 사람의 숫자 ID로는 정상 표시됨."
  severity: major
  test: 1
  artifacts: []
  missing: []

- truth: "Follow action persists after page refresh"
  status: failed
  reason: "User reported: 팔로우 후 UI는 즉시 반응하지만, 새로고침하면 팔로우 상태가 사라짐. 백엔드에 실제로 저장되지 않거나 팔로우 상태 재조회가 잘못된 것으로 보임."
  severity: major
  test: 5
  artifacts: []
  missing: []

- truth: "Neighbors modal shows followers in Followers tab and following in Following tab with correct counts"
  status: failed
  reason: "User reported: 상대방을 팔로우한 후 상대방의 모달을 열면, 나(팔로워)가 팔로워 탭이 아닌 팔로잉 탭에 표시됨. 팔로잉 카운트가 1 증가함 (팔로워 카운트가 증가해야 함)."
  severity: major
  test: 6
  artifacts: []
  missing: []
