---
status: diagnosed
phase: 06-walk-threads
source: 06-UAT.md (gap retest after 06-04, 06-05, 06-06 fixes)
started: 2026-03-06T14:00:00Z
updated: 2026-03-06T16:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. INDIVIDUAL 선택 시 최대 참여인원 자동 2명
expected: RECRUIT 탭에서 채팅 유형을 INDIVIDUAL(1:1)로 선택하면 최대 참여인원이 자동으로 2명으로 설정됨. GROUP 선택 시 5명으로 변경됨.
result: issue
reported: "1:1인 경우 자동으로 2명으로 설정이 되어있지만 마우스를 가져다다니 화살표가 생기면서 숫자를 변경할 수가 있게 되어 있어. 1:1에서는 해당 변경을 완전히 막고 group에서만 허용하는 것이 맞는 것 같아."
severity: minor
fix: INDIVIDUAL 시 input disabled + 안내 문구 추가 (즉시 수정 완료)

### 2. 지도 확대/축소
expected: FIND 탭 지도에서 +/- 줌 컨트롤 버튼이 보이고, 마우스 휠이나 핀치로 확대/축소가 가능함. 축소도 충분히 가능 (최소 zoom 12 이하).
result: pass

### 3. 스레드 수정 제출 (duplicate key 해결)
expected: 기존 스레드를 수정하여 제출하면 에러 없이 성공. 펫 선택을 변경해도 duplicate key 에러 없이 정상 업데이트됨.
result: pass

### 4. 스레드 삭제 후 새 스레드 생성
expected: 스레드를 삭제한 뒤 RECRUIT 탭으로 이동하면 빈 폼이 정상 표시됨. T404 에러 없이 새 스레드를 생성할 수 있음.
result: pass

### 5. 핫스팟 마커 클릭 팝업
expected: FIND 탭 지도에서 핫스팟(불꽃) 마커를 클릭하면 지역명과 스레드 수를 담은 팝업이 표시됨. 팝업이 즉시 사라지지 않고 유지됨.
result: pass
note: "클릭 없이도 팝업이 계속 유지됨 — 사용자 판단으로 이대로 OK"

### 6. 신청 상태 표시 (applied 필드)
expected: 이미 신청한 스레드의 상세 팝업에서 "신청 취소" 버튼이 표시됨. "산책 신청하기" 버튼이 아닌 취소 버튼이 보여야 함.
result: pass

### 7. 팝업 날짜 표시
expected: 스레드 상세 팝업에서 시작/종료 시간 앞에 날짜(walkDate)도 함께 표시됨. 예: "2026-03-07 14:00 ~ 15:00"
result: pass

### 8. 삭제 확인 시각적 강조
expected: 삭제하기 버튼 클릭 후 확인 단계에서 빨간색 배경/경고 영역이 나타나고 "삭제하면 되돌릴 수 없습니다" 등의 경고 문구가 표시되어 상태 변화가 명확히 인지됨.
result: pass

### 9. 활성 스레드 보유 시 RECRUIT 탭 차단
expected: 이미 활성 스레드가 있는 사용자가 RECRUIT 탭을 열면 폼 대신 "이미 활성 스레드가 있습니다" 안내 배너가 표시됨.
result: pass

### 10. 남은 시간 일/시간/분 형식
expected: 지도 팝업과 사이드바의 남은 시간이 "30분 남음" 대신 적절한 단위로 분해되어 표시됨. 예: "1일 2시간 30분", "2시간 15분", "45분".
result: pass

### 11. 날짜 범위 필터
expected: 헤더(재탐색 버튼 근처)에 날짜 범위 선택 UI가 있음. 시작/종료 날짜를 지정하면 해당 범위의 스레드만 목록과 지도에 표시됨.
result: issue
reported: "날짜 입력 시 즉시 서버 요청 발생 + 백엔드 파라미터 타입 에러. 개선 요청: (1) 날짜+동네+반경 설정 후 재탐색 버튼으로만 필터링, (2) 반경 조절 1~100km, (3) 동네 설정 위치 기준으로 재탐색, (4) 지도+사이드바 동일 결과"
severity: major

### 12. 만료 스레드 미표시
expected: 목록(사이드바)에서 만료된 스레드가 기본적으로 표시되지 않음. 만료된 스레드가 회색으로 보이거나 목록에서 사라짐.
result: issue
reported: "시간이 지난 스레드들은 안보이는 것이 맞지만, 정원이 모두 찬 스레드는 숨기지 말고 보여주면서 '모집 완료' 배지로 표기해야 함. '만료됨'과 '모집 완료'를 구분해야 함."
severity: minor

### 13. 내 스레드 수정 시 활성 스레드 차단 우회
expected: 내가 작성한 스레드의 수정 버튼을 눌러 RECRUIT 탭으로 이동하면, 활성 스레드 차단 배너 없이 수정 폼이 표시됨.
result: issue
reported: "내가 작성한 스레드를 수정할려고 하니까 이미 작성한 스레드이 있다는 알림창으로 이동이 되버려."
severity: blocker

### 14. 헤더 재탐색 버튼 위치
expected: 재탐색 버튼이 동네 설정, 날짜, radius 뒤에 위치하여 모든 필터 설정 후 마지막에 누를 수 있음.
result: issue
reported: "헤더부분에 재탐색버튼은 동네 설정하기, 날짜, radius 다음에 두는 것이 어떨까?"
severity: minor

### 15. Radius에 따른 지도 원형 크기 변경
expected: radius 설정값에 따라 지도상의 원형(Circle) 크기가 알맞게 변함. 예: 1km 선택 시 작은 원, 100km 선택 시 큰 원.
result: issue
reported: "radius 설정한 값에 따라서 지도상에 원형의 크기가 알맞게 변했으면 좋겠어."
severity: major

### 16. 동네 설정 초기화 시 "현재 위치" 표시
expected: 새로고침으로 GPS 위치로 돌아간 경우, 동네 설정하기 버튼에 이전 검색 주소가 아닌 "현재 위치"로 표시됨.
result: issue
reported: "동네 설정하기를 해서 다른 곳을 탐색한 경우 새로고침을 하면 나의 위치로 돌아가지만 동네 설정하기에는 이전에 설정한 위치값이 남아있어. 나의 위치로 지도가 이동했다면 '현재 위치' 이렇게 표시되었으면 좋겠는데."
severity: minor

### 17. 지도 드래그 탐색 + 역지오코딩
expected: 마우스 드래그로 지도를 자유롭게 이동할 수 있고, 기존 위치로 자동 복귀하지 않음. 드래그 후 대략적인 위치명이 동네설정하기에 반영됨.
result: issue
reported: "지도상에서 마우스를 이용하여 지도를 좌우로 움직여도 다시 기존의 지정된 위치로 계속 돌아가버려. 마우스 드래그를 통해 지도를 여러곳 탐색하는 것이 가능했으면 좋겠어. 그리고 영역의 대략적인 위치명도 동네설정하기란에 반영되었으면 좋겠어."
severity: major

## Summary

total: 17
passed: 9
issues: 7
pending: 0
skipped: 0

## Gaps

- truth: "날짜 범위 필터가 지도 마커에도 동일하게 적용되어야 함"
  status: failed
  reason: "getThreadMap API가 날짜 파라미터를 받지 않아 사이드바는 필터링되지만 지도는 전체 스레드 표시"
  severity: major
  test: 11
  root_cause: "fetchThreadData에서 getThreadMap 호출 시 startDate/endDate 미전달. 백엔드 /threads/map 엔드포인트도 날짜 파라미터 미지원"
  artifacts:
    - path: "aini-inu-frontend/src/hooks/useRadarLogic.ts"
      issue: "getThreadMap 호출에 date params 누락 (line 89)"
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/controller/WalkThreadController.java"
      issue: "/threads/map 엔드포인트에 startDate/endDate 파라미터 없음 (lines 100-110)"
    - path: "aini-inu-backend/src/main/java/scit/ainiinu/walk/service/WalkThreadService.java"
      issue: "getMapThreads에 날짜 필터 없음 (lines 135-170)"
  missing:
    - "백엔드: /threads/map에 startDate/endDate @RequestParam 추가 + getMapThreads에 날짜 필터링"
    - "프론트: getThreadMap 호출 시 dateFrom/dateTo 전달"
  debug_session: ".planning/debug/uat11-date-range-filter.md"

- truth: "페이지네이션(더 보기)이 위치 필터를 유지해야 함"
  status: failed
  reason: "loadMore 함수가 latitude/longitude/radius를 getThreads에 전달하지 않아 2페이지부터 전체 위치 스레드 반환"
  severity: major
  test: 11
  root_cause: "loadMore(useRadarLogic.ts:169-180)가 getThreads에 dateFrom/dateTo만 전달하고 위치 파라미터 누락"
  artifacts:
    - path: "aini-inu-frontend/src/hooks/useRadarLogic.ts"
      issue: "loadMore에서 latitude/longitude/radius 미전달 (lines 169-180)"
  missing:
    - "loadMore에서 getThreads 호출 시 searchCoordinates ?? coordinates + radius 전달"
  debug_session: ".planning/debug/uat11-date-range-filter.md"

- truth: "정원이 찬 스레드는 숨기지 않고 '모집 완료' 배지로 표시, 시간 만료 스레드만 숨김"
  status: failed
  reason: "User reported: 정원이 모두 찬 스레드는 숨기지 말고 보여주면서 '모집 완료' 배지로 표기해야 함"
  severity: minor
  test: 12
  root_cause: "시스템에 '정원 초과' 개념 없음. WalkThreadStatus에 FULL 없고, 프론트엔드도 currentParticipants >= maxParticipants 비교 없이 동일하게 렌더링. 신청 버튼도 정원 찬 스레드에서 비활성화되지 않음"
  artifacts:
    - path: "aini-inu-frontend/src/components/around-me/RadarSidebar.tsx"
      issue: "배지 로직에 '모집 완료' 상태 없음 (lines 159-174)"
    - path: "aini-inu-frontend/src/components/around-me/RadarMapSection.tsx"
      issue: "팝업에 '모집 완료' 배지 없고, 정원 찬 스레드에도 신청 버튼 활성화"
  missing:
    - "RadarSidebar: currentParticipants >= maxParticipants일 때 '모집 완료' 배지 표시 (파란색 등 구분)"
    - "RadarMapSection: 팝업에 '모집 완료' 배지 + 신청 버튼 비활성화"
  debug_session: ".planning/debug/uat12-full-vs-expired-thread.md"

- truth: "내 스레드 수정 시 활성 스레드 차단을 우회해야 함"
  status: failed
  reason: "수정 버튼 → RECRUIT 탭 이동 시 myActiveThread가 있으므로 폼 대신 차단 배너가 표시됨"
  severity: blocker
  test: 13
  root_cause: "page.tsx:117-155에서 RECRUIT 탭 렌더링이 myActiveThread 존재 여부만 체크. editingThreadId가 set되어 있어도 myActiveThread 차단이 우선 적용되어 수정 폼에 도달 불가"
  artifacts:
    - path: "aini-inu-frontend/src/app/around-me/page.tsx"
      issue: "RECRUIT 탭 조건이 myActiveThread만 체크, editingThreadId 무시 (lines 117-155)"
  missing:
    - "조건을 (myActiveThread && !editingThreadId)로 변경하여 수정 모드에서는 차단 우회"

- truth: "재탐색 버튼이 필터 UI 뒤에 위치해야 함"
  status: failed
  reason: "현재 순서: 동네설정 → 재탐색 → 날짜 → radius. 사용자 기대: 동네설정 → 날짜 → radius → 재탐색"
  severity: minor
  test: 14
  root_cause: "AroundMeHeader.tsx:49-95에서 onRefresh 버튼이 onLocationClick 바로 뒤(line 53), 날짜/radius 앞에 렌더링됨"
  artifacts:
    - path: "aini-inu-frontend/src/components/around-me/AroundMeHeader.tsx"
      issue: "재탐색 버튼이 날짜/radius 필터 앞에 위치 (line 53)"
  missing:
    - "재탐색 버튼 JSX를 날짜+radius 블록 뒤로 이동"

- truth: "지도 원형(Circle) radius가 설정값과 연동되어야 함"
  status: failed
  reason: "DynamicMap의 Circle radius가 2500m 하드코딩"
  severity: major
  test: 15
  root_cause: "DynamicMap.tsx:104에서 Circle radius={2500} 고정. 컴포넌트에 radiusKm prop이 없음. RadarMapSection도 radius 값을 DynamicMap에 전달하지 않음"
  artifacts:
    - path: "aini-inu-frontend/src/components/common/DynamicMap.tsx"
      issue: "Circle radius={2500} 하드코딩 (line 104)"
    - path: "aini-inu-frontend/src/components/around-me/RadarMapSection.tsx"
      issue: "DynamicMap에 radius prop 미전달"
  missing:
    - "DynamicMap에 radiusKm prop 추가, Circle radius={radiusKm * 1000}로 변경"
    - "RadarMapSection에 radius prop 추가하여 DynamicMap에 전달"
    - "page.tsx에서 RadarMapSection에 radius 전달"

- truth: "GPS 위치 사용 시 동네설정에 '현재 위치' 표시"
  status: failed
  reason: "새로고침 시 searchCoordinates는 null로 초기화되지만 useConfigStore의 currentLocation은 persist되어 이전 검색 주소가 남음"
  severity: minor
  test: 16
  root_cause: "useConfigStore가 Zustand persist로 currentLocation을 localStorage에 저장. 새로고침 시 searchCoordinates=null이지만 currentLocation은 이전 값 유지. GPS 좌표 사용 시 '현재 위치'로 초기화하는 로직 없음"
  artifacts:
    - path: "aini-inu-frontend/src/hooks/useRadarLogic.ts"
      issue: "GPS 초기화 시 configStore.setLocation('현재 위치') 호출 없음 (lines 107-131)"
    - path: "aini-inu-frontend/src/store/useConfigStore.ts"
      issue: "currentLocation이 persist되어 새로고침 후에도 이전 값 유지"
  missing:
    - "GPS 위치 획득 성공/fallback 시 setLocation('현재 위치') 호출"
    - "또는 searchCoordinates가 null일 때 currentLocation을 '현재 위치'로 표시"

- truth: "지도 드래그 자유 탐색이 가능해야 하며 위치가 자동 복귀하지 않아야 함"
  status: failed
  reason: "MapController의 useEffect가 center prop 변경 시 setView + setMaxBounds를 호출하여 지도를 강제 복귀시킴"
  severity: major
  test: 17
  root_cause: "DynamicMap.tsx:18-44의 MapController가 center 변경 시마다 map.setView(center)와 map.setMaxBounds(center 기준 5km)를 호출. 드래그로 이동해도 React 리렌더 시 center prop이 동일하게 전달되어 복귀. 또한 maxBoundsViscosity={1.0}으로 경계 밖 이동 완전 차단"
  artifacts:
    - path: "aini-inu-frontend/src/components/common/DynamicMap.tsx"
      issue: "MapController가 center 변경마다 setView+setMaxBounds 강제 (lines 18-44), maxBoundsViscosity=1.0 (line 91)"
  missing:
    - "MapController에서 setMaxBounds 제거 또는 대폭 완화"
    - "maxBoundsViscosity 제거 또는 0으로 변경"
    - "지도 드래그 종료(moveend) 이벤트 시 중심 좌표의 역지오코딩으로 동네설정 업데이트"
    - "역지오코딩: Kakao/Naver reverse geocode API 또는 Nominatim(무료) 사용"
