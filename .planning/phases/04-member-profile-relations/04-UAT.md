---
status: complete
phase: 04-member-profile-relations
source: [04-01-SUMMARY.md, 04-02-SUMMARY.md, 04-03-SUMMARY.md, 04-04-SUMMARY.md, 04-05-SUMMARY.md, 04-06-SUMMARY.md]
started: 2026-03-06T06:10:00Z
updated: 2026-03-06T06:30:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Own Profile Page Load
expected: Navigate to your own profile page (/profile/me or /profile/[your-id]). The page shows your ProfileHeader with your name, avatar, follower count, and following count. No error state ("프로필을 불러오는데 실패했습니다") and no stuck loading spinner.
result: pass

### 2. Walk Heatmap
expected: On your own profile page, below your ProfileHeader, a GitHub-style heatmap grid of walk activity is visible. Each cell represents a day; days with walks show a colored square (darker = more walks). No errors or blank space where the heatmap should be.
result: pass

### 3. Profile Edit Modal
expected: Click the Edit button on your own profile. A modal opens with all profile fields: nickname, linked nickname, profile image URL, phone, age, gender (MALE/FEMALE/UNKNOWN), MBTI, self-introduction, and personality type chips (multi-select). Clicking a personality chip toggles it. Save submits and the profile updates.
result: pass

### 4. Other Member Profile
expected: Navigate to another member's profile (/profile/[other-id]). The page shows their ProfileHeader with their name, avatar, and a Follow button (or Unfollow if already following). Their registered dogs appear in the DOGS tab.
result: pass

### 5. Follow / Unfollow Toggle
expected: On another member's profile, click Follow. The button switches to Unfollow immediately (optimistic update) and the follower count increments. Refresh the page — the Unfollow state persists (follow state survives refresh). Click Unfollow — button reverts to Follow.
result: pass

### 6. Neighbors Modal (Followers / Following List)
expected: Click the follower or following count on another member's profile. A modal opens showing THAT MEMBER's followers/following — not your own list. If you are in their followers, you appear in the Followers tab (not the Following tab).
result: pass

### 7. Member Search from Sidebar
expected: Click the Search button in the sidebar (desktop: icon above the "+" button; mobile: Search item in the bottom nav). A search modal opens with an auto-focused input. Type a member name — results appear within ~300ms with avatar, nickname, and manner temperature badge. Clicking a result navigates to their profile and closes the modal.
result: pass

### 8. Own Profile DOGS Tab
expected: On your own profile page, click the DOGS tab. Your registered pets appear with their names and photos. The list does NOT show an error or empty state if you have registered pets.
result: pass

## Summary

total: 8
passed: 8
issues: 0
pending: 0
skipped: 0

## Gaps

[none]
