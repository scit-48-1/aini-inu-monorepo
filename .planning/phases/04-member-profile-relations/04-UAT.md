---
status: resolved
phase: 04-member-profile-relations
source: [04-01-SUMMARY.md, 04-02-SUMMARY.md, 04-03-SUMMARY.md, 04-04-SUMMARY.md]
started: 2026-03-06T02:00:00Z
updated: 2026-03-06T06:00:00Z
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
  status: resolved
  reason: "User reported: /profile/me도 실패, DB에서 확인한 본인 숫자 ID(/profile/[내 ID])로 접속해도 MyProfileView에서 '프로필을 불러오는데 실패했습니다.' 에러 상태와 다시시도 버튼이 표시됨. 다른 사람의 숫자 ID로는 정상 표시됨."
  severity: major
  test: 1
  root_cause: "postService.getPosts()가 인증 헤더가 없는 레거시 apiClient(src/services/api/apiClient.ts)를 사용하여 GET /posts가 401을 반환하고, MyProfileView의 Promise.all 전체가 실패하여 에러 UI가 표시됨. OtherProfileView는 postService를 사용하지 않아 정상 동작."
  artifacts:
    - path: "aini-inu-frontend/src/services/api/postService.ts"
      issue: "레거시 apiClient (인증 없음) 사용. @/api/client (인증 포함)로 교체해야 함"
    - path: "aini-inu-frontend/src/components/profile/MyProfileView.tsx"
      issue: "postService.getPosts()를 Promise.all 내부에 포함시켜, 이 호출 실패 시 전체 프로필 로드가 실패함"
  missing:
    - "postService.ts를 @/api/client 기반으로 교체하거나, getPosts()를 Promise.all 밖으로 분리하여 실패해도 프로필이 렌더링되도록 처리"
  debug_session: "/private/tmp/claude-501/-Users-keonhongkoo-Desktop-github-aini-inu/tasks/aa6c37361d8f9a1db.output"

- truth: "Follow action persists after page refresh"
  status: resolved
  reason: "User reported: 팔로우 후 UI는 즉시 반응하지만, 새로고침하면 팔로우 상태가 사라짐. 백엔드에 실제로 저장되지 않거나 팔로우 상태 재조회가 잘못된 것으로 보임."
  severity: major
  test: 5
  root_cause: "OtherProfileView의 fetchFollowState가 getFollowing({size:100}) 결과를 선형 스캔하여 팔로우 상태를 판별함. 100명 초과 팔로잉 시 대상이 목록에 없어 isFollowing이 false로 초기화됨. 백엔드에는 전용 엔드포인트(GET /members/me/follows/{targetId})가 있지만 프론트엔드가 이를 사용하지 않음."
  artifacts:
    - path: "aini-inu-frontend/src/components/profile/OtherProfileView.tsx"
      issue: "fetchFollowState가 getFollowing({size:100}) 리스트 스캔 방식 사용. 100명 초과 시 오동작"
    - path: "aini-inu-frontend/src/api/members.ts"
      issue: "getFollowStatus(targetId) 함수 미정의. GET /api/v1/members/me/follows/{targetId} 엔드포인트 미활용"
  missing:
    - "src/api/members.ts에 getFollowStatus(targetId: number) 함수 추가"
    - "OtherProfileView.fetchFollowState를 getFollowStatus(memberId) 호출로 교체"
  debug_session: "/private/tmp/claude-501/-Users-keonhongkoo-Desktop-github-aini-inu/tasks/a5f9c0174ba0333ef.output"

- truth: "Neighbors modal shows followers in Followers tab and following in Following tab with correct counts"
  status: resolved
  reason: "User reported: 상대방을 팔로우한 후 상대방의 모달을 열면, 나(팔로워)가 팔로워 탭이 아닌 팔로잉 탭에 표시됨. 팔로잉 카운트가 1 증가함 (팔로워 카운트가 증가해야 함)."
  severity: major
  test: 6
  root_cause: "getFollowers()/getFollowing()이 /members/me/followers, /members/me/following을 고정 호출하여 항상 현재 로그인 사용자의 목록을 반환함. NeighborsModal이 memberId를 받지 않아 상대방 프로필에서 열어도 내 팔로워/팔로잉이 표시됨. 또한 OtherProfileView의 followerCount 초기값이 0으로 하드코딩되어 실제 팔로워 수를 반영하지 못함."
  artifacts:
    - path: "aini-inu-frontend/src/api/members.ts"
      issue: "getFollowers()/getFollowing()이 memberId 파라미터 없이 /members/me/... 고정 호출"
    - path: "aini-inu-frontend/src/components/profile/NeighborsModal.tsx"
      issue: "memberId prop을 받지 않아 상대방의 팔로워/팔로잉을 조회할 수 없음"
    - path: "aini-inu-frontend/src/components/profile/OtherProfileView.tsx"
      issue: "followerCount 초기값이 0으로 하드코딩. 상대방의 실제 팔로워 수를 API로 가져오지 않음"
  missing:
    - "getFollowers(memberId)/getFollowing(memberId) 함수에 memberId 파라미터 추가 (또는 오버로드)"
    - "NeighborsModal에 memberId prop 추가하여 대상 멤버의 목록을 조회"
    - "OtherProfileView에서 상대방의 실제 followerCount를 가져오는 로직 추가"
  debug_session: "/private/tmp/claude-501/-Users-keonhongkoo-Desktop-github-aini-inu/tasks/ad8e607efe3aefc6a.output"
