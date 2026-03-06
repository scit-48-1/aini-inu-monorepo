---
status: diagnosed
phase: 06-walk-threads
source: 06-01-SUMMARY.md, 06-02-SUMMARY.md, 06-03-SUMMARY.md
started: 2026-03-06T10:00:00Z
updated: 2026-03-06T11:00:00Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. GPS Acquisition with Fallback
expected: Navigate to /around-me. The page requests your GPS location. If you allow it, map centers on your real location. If you deny/block it, map falls back to Seoul City Hall (37.5663, 126.9779) — the map still loads and shows threads.
result: pass

### 2. Manual Refresh Button
expected: A "재탐색" (re-search) button with a refresh icon appears in the header. Clicking it reloads thread data and the icon spins during the fetch. The page does NOT auto-refresh every 10 seconds on its own.
result: pass

### 3. EMERGENCY Tab Disabled
expected: The EMERGENCY tab exists in the tab bar but appears visually muted (grayed out / reduced opacity). Clicking it shows a "준비 중" (coming soon) overlay rather than functional content.
result: pass

### 4. Non-Pet-Owner Block
expected: Log in as a user with no registered pets and navigate to /around-me → RECRUIT tab. Instead of the form, a block card appears with an alert icon and a link to /profile. The form itself is not shown.
result: pass

### 5. RecruitForm — Create Thread
expected: Log in as a user with at least one pet. On /around-me → RECRUIT tab, you see a form with: title input, walk date picker, start time, end time, chat type toggle (INDIVIDUAL/GROUP), max participants, location (address search via Daum Postcode), and pet selection cards showing your pet photos/names. Fill all required fields and submit — a new thread is created.
result: issue
reported: "다 좋은데, 1대1 만남을 선택한 경우 최대 참여인원이 5명으로 기본 고정되어 있어서 내가 2명으로 설정을 해야 스레드를 생성할 수 있는 문제가 있어. 아 그리고 지도가 확대 축소가 안되는 문제도 있다."
severity: major

### 6. RecruitForm — Address Search
expected: In the RECRUIT form, clicking the address search area opens a Daum Postcode inline search panel inside the form (not a separate browser popup). Searching and selecting an address fills in the location field.
result: pass

### 7. RecruitForm — ChatType Toggle
expected: The INDIVIDUAL and GROUP buttons in the form are styled distinctly. Hovering shows a tooltip explaining the difference. Only one can be active at a time.
result: pass

### 8. RecruitForm — Edit Mode Pre-fill
expected: When editing an existing thread (e.g., via the map popup Edit button), the form opens pre-filled with all the thread's existing values — title, date, times, chat type, location, pet selections. Submitting updates the thread.
result: issue
reported: "다 괜찮은데, 스레드를 수정하고 제출한 경우 모집글 수정에 실패했다고 나타나. 백엔드에서 로그를 확인해보니 duplicate key value violates unique constraint ukkn2yxrid07y2f1exhd2anf6ex — Key (thread_id, pet_id)=(9403, 9307) already exists. 그리고 이미 활성스레드가 있는 사람은 처음부터 스레드 활성을 막는 것이 어떨까? 그리고 남은 시간이 분으로 표현되니까 이해하기 어려워. 몇일 몇시간 몇분 이렇게 표현하는 것이 좋아보여."
severity: blocker

### 9. Thread Markers on FIND Map
expected: On /around-me → FIND tab, nearby walk threads appear as marker pins on the map. Each marker corresponds to a thread's location.
result: pass

### 10. Hotspot Markers
expected: Seoul district hotspots (areas with many threads) appear as flame-style markers on the map. Clicking a hotspot marker shows a popup with the district name and thread count. Non-Seoul or unrecognized regions appear in an overlay panel instead.
result: issue
reported: "클릭해도 아무것도 안나타나."
severity: major

### 11. Thread Detail Popup
expected: Clicking a thread marker on the map opens a popup showing: thread title, chatType badge (1:1 or 그룹), expiry status, place name + address, start and end time, remaining minutes, and current participant count.
result: issue
reported: "다른건 다 정상작동해 그리고 start, end타임은 뜨는데 날짜가 안떠서 불편해."
severity: minor

### 12. Apply to Thread
expected: In the thread detail popup (for a thread you haven't applied to and don't own), an "산책 신청하기" button appears. Clicking it shows a pet selection step — select your pet(s) and confirm. A success toast appears with an option to navigate to the chat room.
result: pass

### 13. Cancel Application
expected: In the detail popup for a thread you've already applied to, a "신청 취소" button appears instead of apply. Clicking it cancels your application and shows a confirmation toast.
result: issue
reported: "이미 산책중인 스레드에도 산책신청이 가능하다고 나타나며 실제로 참여도 가능해. 물론 같은 사람이 같은 방에 또 들어가는 것이라 그런지 에러는 안나타나."
severity: major

### 14. Owner Edit / Delete in Popup
expected: In the detail popup for a thread YOU created, Edit and Delete buttons appear. Delete shows a confirmation dialog before proceeding. Non-owners do not see these buttons.
result: issue
reported: "삭제하기 버튼을 누르니까 삭제확인 버튼으로 바뀌고 다시 한번더 누르니까 삭제가 이루어지네. 순간 삭제하기에서 삭제확인으로 바뀐줄 몰랐어. 조금 더 눈에 띄는 방법 없을까?"
severity: minor

### 15. Sidebar Thread Cards
expected: The FIND tab sidebar lists nearby threads as cards. Each card shows: title, truncated description, place name, distance from your location, remaining time badge (e.g., "30분 남음" or "만료됨"), participant count, chatType badge, and a green "참여 중" badge if you've already applied.
result: pass

### 16. Sidebar Sort Toggle
expected: The sidebar has DISTANCE and TIME sort buttons. DISTANCE sorts threads by proximity to your location. TIME sorts by walk start time. Switching toggles re-orders the list immediately.
result: pass
note: "User requested: 날짜 범위 필터 추가 (헤더 근처), 만료 스레드 기본 미표시"

### 17. Sidebar Load More
expected: If there are more threads than the initial page, a "더 보기" (load more) button appears at the bottom of the sidebar. Clicking it appends more threads to the list. The button disappears when all threads are loaded.
result: pass

## Summary

total: 17
passed: 11
issues: 6
pending: 0
skipped: 0

## Gaps

- truth: "INDIVIDUAL 채팅 유형 선택 시 최대 참여인원이 자동으로 2명으로 설정되어야 함"
  status: failed
  reason: "User reported: 1대1 만남을 선택한 경우 최대 참여인원이 5명으로 기본 고정되어 있어서 내가 2명으로 설정을 해야 스레드를 생성할 수 있는 문제가 있어"
  severity: major
  test: 5
  root_cause: "chatType 토글 onClick 핸들러(RecruitForm.tsx:405)가 chatType만 업데이트하고 maxParticipants를 연동하지 않음. 초기값도 maxParticipants=5로 INDIVIDUAL 기본값과 불일치"
  artifacts:
    - path: "aini-inu-frontend/src/components/around-me/RecruitForm.tsx"
      issue: "chatType 변경 시 maxParticipants 자동 갱신 로직 없음 (line 405)"
  missing:
    - "chatType onClick 핸들러에서 INDIVIDUAL 선택 시 maxParticipants를 2로, GROUP 선택 시 5로 co-update"
    - "초기 maxParticipants 값을 2로 변경 (기본 chatType이 INDIVIDUAL이므로)"
  debug_session: ""

- truth: "지도에서 확대/축소가 가능해야 함"
  status: failed
  reason: "User reported: 지도가 확대 축소가 안되는 문제도 있다"
  severity: major
  test: 5
  root_cause: "DynamicMap.tsx에서 zoomControl={false}로 +/- 버튼이 제거됨. 또한 minZoom={14}가 초기 zoom={14}와 동일하여 축소 불가"
  artifacts:
    - path: "aini-inu-frontend/src/components/common/DynamicMap.tsx"
      issue: "zoomControl={false} + minZoom={14} 조합으로 zoom UI 없음 (lines 81-83)"
  missing:
    - "zoomControl={false} 제거 또는 true로 변경"
    - "minZoom을 12 또는 13으로 낮춰 축소 허용"
  debug_session: ""

- truth: "스레드 수정(edit) 제출 시 기존 pet 연결을 교체해야 하며 에러 없이 성공해야 함"
  status: failed
  reason: "User reported: 스레드를 수정하고 제출한 경우 모집글 수정에 실패. 백엔드 로그: duplicate key value violates unique constraint ukkn2yxrid07y2f1exhd2anf6ex — Key (thread_id, pet_id)=(9403, 9307) already exists."
  severity: blocker
  test: 8
  root_cause: "WalkThreadPetRepository.deleteAllByThreadId가 @Modifying(clearAutomatically=true) 없이 bulk DELETE를 수행하여 Hibernate 1차 캐시가 클리어되지 않음. 이후 saveThreadPets의 save() 호출 시 DB에 이전 row가 아직 존재하는 것처럼 처리되어 unique constraint 위반 발생"
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/repository/WalkThreadPetRepository.java"
      issue: "deleteAllByThreadId에 @Modifying(clearAutomatically=true) 누락 (line 13)"
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java"
      issue: "updateThread에서 delete 후 flush 없이 바로 saveThreadPets 호출 (lines 180-181)"
  missing:
    - "@Modifying(clearAutomatically = true)를 deleteAllByThreadId에 추가, 또는 delete 후 flush() 명시 호출"
  debug_session: ""

- truth: "스레드 삭제 후 새 스레드 생성 시 T404 에러 없이 정상적으로 폼이 열려야 함"
  status: failed
  reason: "User reported: 모집글을 삭제하고 다시 생성할려하니 스레드를 찾을수없다는 에러와 모집글 수정에 실패했다는 에러. 백엔드 로그: Business Exception: code=T404_THREAD_NOT_FOUND"
  severity: blocker
  test: 9
  root_cause: "handleDeleteThread(useRadarLogic.ts:193-202)에서 editingThreadId를 null로 초기화하지 않음. 삭제 후 RECRUIT 탭으로 이동 시 RecruitForm의 useEffect(line 73)가 삭제된 threadId로 getThread() 호출하여 T404 발생"
  artifacts:
    - path: "aini-inu-frontend/src/hooks/useRadarLogic.ts"
      issue: "handleDeleteThread에 setEditingThreadId(null) 누락 (line 193-202)"
  missing:
    - "handleDeleteThread 내부에 setEditingThreadId(null) 추가"
  debug_session: ""

- truth: "핫스팟 마커 클릭 시 지역명과 스레드 수를 담은 팝업이 표시되어야 함"
  status: failed
  reason: "User reported: 클릭해도 아무것도 안나타나."
  severity: major
  test: 10
  root_cause: "RadarMapSection.tsx:234의 지도 wrapper div의 onClick이 setHotspotPopup(null)을 무조건 호출함. Leaflet 마커 클릭 이벤트가 상위 div로 버블링되어 handleMapMarkerClick에서 방금 set한 hotspotPopup state를 즉시 null로 초기화"
  artifacts:
    - path: "aini-inu-frontend/src/components/around-me/RadarMapSection.tsx"
      issue: "wrapper div onClick이 setHotspotPopup(null) 무조건 호출 — 마커 클릭 이벤트 버블링으로 팝업 즉시 소멸 (line 234)"
  missing:
    - "wrapper onClick에서 hotspotPopup이 이미 있을 때만 null로 설정하거나, 마커 클릭 이벤트의 버블링 차단"
  debug_session: ""

- truth: "이미 신청한 스레드 팝업에서는 '신청 취소' 버튼이 표시되어야 하며 '산책 신청하기' 버튼이 보여선 안 됨"
  status: failed
  reason: "User reported: 이미 산책중인 스레드에도 산책신청이 가능하다고 나타나며 실제로 참여도 가능해."
  severity: major
  test: 13
  root_cause: "백엔드 ThreadResponse.from()이 applicants를 항상 List.of() (빈 리스트)로 세팅. 프론트 RadarMapSection에서 isApplied를 applicants.some()으로 판별하므로 항상 false. ThreadResponse에 applied boolean 필드 자체가 없음 (ThreadSummaryResponse에는 있음)"
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/dto/response/ThreadResponse.java"
      issue: "from()에서 applicants: List.of() 하드코딩, applied boolean 필드 없음 (line 76)"
    - path: "aini-inu-frontend/src/api/threads.ts"
      issue: "ThreadResponse 타입에 applied/isApplied 필드 없음 (lines 53-73)"
    - path: "aini-inu-frontend/src/components/around-me/RadarMapSection.tsx"
      issue: "isApplied를 applicants.some()으로 판별하나 applicants는 항상 빈 배열 (line 220)"
  missing:
    - "백엔드 GET /threads/{threadId}에서 신청자 목록 또는 applied boolean을 현재 사용자 기준으로 채워서 반환"
    - "프론트 ThreadResponse 타입에 applied: boolean 추가"
    - "RadarMapSection isApplied 로직을 selectedThread.applied로 단순화"
  debug_session: ""

- truth: "스레드 상세 팝업에서 시작/종료 시간과 함께 날짜도 표시되어야 함"
  status: failed
  reason: "User reported: start, end타임은 뜨는데 날짜가 안떠서 불편해."
  severity: minor
  test: 11
  root_cause: "formatTime 함수(RadarMapSection.tsx:198-204)가 toLocaleTimeString으로 시간만 추출. walkDate 필드가 ThreadResponse에 존재하지만 팝업 렌더링(line 315)에서 참조하지 않음"
  artifacts:
    - path: "aini-inu-frontend/src/components/around-me/RadarMapSection.tsx"
      issue: "formatTime이 시간만 반환, walkDate 미사용 (lines 198-204, 315)"
  missing:
    - "팝업 시간 표시에 selectedThread.walkDate를 prefix로 추가: '{walkDate} {startTime} ~ {endTime}'"
  debug_session: ""

- truth: "삭제 확인 단계가 사용자에게 명확히 인지될 수 있어야 함"
  status: failed
  reason: "User reported: 삭제하기 버튼에서 삭제확인으로 바뀐줄 몰랐어."
  severity: minor
  test: 14
  root_cause: "isConfirmingDelete 상태 전환 시 버튼 텍스트만 변경되고 시각적 경고(배경색, 경고 문구 등)가 없어 사용자가 상태 변화를 인지하기 어려움 (RadarMapSection.tsx:343-385)"
  artifacts:
    - path: "aini-inu-frontend/src/components/around-me/RadarMapSection.tsx"
      issue: "isConfirmingDelete 시 경고 텍스트/배경색 없음 (lines 343-385)"
  missing:
    - "확인 상태에서 '삭제하면 되돌릴 수 없습니다' 경고 문구 추가"
    - "확인 영역에 bg-red-50 배경 또는 border-red 강조 추가"
  debug_session: ""

- truth: "이미 활성 스레드가 있는 사용자는 새 스레드 생성 시작 전 차단되어야 함"
  status: failed
  reason: "User requested: 이미 활성스레드가 있는 사람은 처음부터 스레드 활성을 막는 것이 어떨까"
  severity: major
  test: 8
  root_cause: "백엔드에 GET /threads/my/active 엔드포인트 없음. 프론트 useRadarLogic이 현재 사용자의 활성 스레드 데이터를 fetch하지 않아 RECRUIT 탭이 무조건 폼을 표시. 백엔드는 POST 시 409를 던지지만 프론트엔드 사전 차단 없음"
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkThreadController.java"
      issue: "GET /threads/my/active 엔드포인트 없음"
    - path: "aini-inu-frontend/src/hooks/useRadarLogic.ts"
      issue: "myActiveThread 데이터 fetch 없음"
    - path: "aini-inu-frontend/src/app/around-me/page.tsx"
      issue: "RECRUIT 탭에서 활성 스레드 보유 여부 체크 없음"
  missing:
    - "백엔드: GET /api/v1/threads/my/active 엔드포인트 추가 (findAllByAuthorIdAndStatus 재사용)"
    - "프론트: useRadarLogic에서 myActiveThread fetch 및 노출"
    - "프론트: RECRUIT 탭에서 myActiveThread 있으면 폼 대신 '이미 활성 스레드가 있습니다' 배너 표시"
  debug_session: ""

- truth: "남은 시간이 'X일 X시간 X분' 형식으로 표시되어야 함"
  status: failed
  reason: "User reported: 남은 시간이 분으로 표현되니까 이해하기 어려워."
  severity: minor
  test: 8
  root_cause: "getRemainingMinutes(RadarMapSection.tsx:206)와 getRemainingBadge(RadarSidebar.tsx:26)가 각각 diff/60000 분 단위만 반환. 일/시간 분해 로직 없음"
  artifacts:
    - path: "aini-inu-frontend/src/components/around-me/RadarMapSection.tsx"
      issue: "getRemainingMinutes: Math.floor(diff/60000)+'분 남음' 만 반환 (lines 206-218)"
    - path: "aini-inu-frontend/src/components/around-me/RadarSidebar.tsx"
      issue: "getRemainingBadge: 동일한 분 단위 포맷 (lines 26-37)"
  missing:
    - "공유 유틸 함수 formatRemainingTime(diff) 생성: days/hours/mins 분해 후 조건부 조합"
    - "두 컴포넌트에서 해당 유틸 사용"
  debug_session: ""

- truth: "날짜 범위 필터를 헤더(재탐색 버튼 근처)에서 지정할 수 있어야 함"
  status: failed
  reason: "User requested: 동네 설정하기, 재탐색 버튼 옆에 날짜 범위를 지정할 수 있도록 하는 것이 어떨까"
  severity: major
  test: 16
  root_cause: "백엔드 GET /threads에 날짜 필터 파라미터 없음. 프론트 getThreads API도 PaginationParams만 수용. 전체 스택에서 날짜 범위 필터 미구현"
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkThreadController.java"
      issue: "getThreads에 startDate/endDate 파라미터 없음"
    - path: "aini-inu-frontend/src/api/threads.ts"
      issue: "getThreads가 날짜 파라미터 미지원"
  missing:
    - "백엔드: walkDate 또는 startDateFrom/To 쿼리 파라미터 + 매칭 Repository 메서드 추가"
    - "프론트: AroundMeHeader에 날짜 범위 picker UI 추가"
    - "프론트: useRadarLogic → getThreads 호출 시 날짜 파라미터 전달"
  debug_session: ""

- truth: "만료된 스레드는 기본적으로 목록에 표시되지 않아야 함"
  status: failed
  reason: "User requested: 만료된 스레드들은 처음부터 안보였으면 좋겠는데"
  severity: major
  test: 16
  root_cause: "WalkThreadService.getThreads가 RECRUITING 상태만 필터링하고 isExpired() 체크를 하지 않음. getMapThreads/getHotspots는 이미 isExpired 필터를 적용하지만 목록 API는 미적용"
  artifacts:
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java"
      issue: "getThreads에 isExpired 필터 없음 (line 103-106)"
  missing:
    - "백엔드: getThreads에서 isExpired(now) 필터 적용 (getMapThreads 패턴 동일하게)"
    - "또는 Repository에 startTime > now-60min 조건 쿼리 추가"
    - "프론트: 만료 스레드 제거 후 RadarSidebar의 opacity-60 조건부 스타일 제거"
  debug_session: ""
