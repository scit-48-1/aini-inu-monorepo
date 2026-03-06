---
status: complete
phase: 06-walk-threads
source: 06-UAT.md (gap retest after 06-04, 06-05, 06-06 fixes)
started: 2026-03-06T14:00:00Z
updated: 2026-03-06T15:00:00Z
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

## Summary

total: 12
passed: 9
issues: 2
pending: 0
skipped: 0

## Gaps

- truth: "정원이 찬 스레드는 숨기지 않고 '모집 완료' 배지로 표시, 시간 만료 스레드만 숨김"
  status: failed
  reason: "User reported: 정원이 모두 찬 스레드는 숨기지 말고 보여주면서 '모집 완료' 배지로 표기해야 함"
  severity: minor
  test: 12
  artifacts: []
  missing: []
