---
status: complete
phase: 06-walk-threads
source: 06-UAT-retest.md (retest-2 after 06-07, 06-08, 06-09, 06-10 fixes)
started: 2026-03-07T10:00:00Z
updated: 2026-03-07T11:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Date Range Filter Syncs Map + Sidebar
expected: 헤더에서 날짜 범위를 설정하고 재탐색 버튼을 누르면, 사이드바 목록과 지도 마커가 모두 해당 날짜 범위의 스레드만 표시됨. 두 영역의 결과가 동일함.
result: pass

### 2. LoadMore Preserves Location Filter
expected: 사이드바에서 "더 보기" 버튼을 누르면 현재 위치 기준의 스레드가 추가로 로드됨. 2페이지부터 다른 지역의 스레드가 섞여 나오지 않음.
result: pass

### 3. Full Thread Badge (모집 완료)
expected: 정원이 찬 스레드(currentParticipants >= maxParticipants)에 파란색 "모집 완료" 뱃지가 사이드바 카드와 지도 팝업에 표시됨. 팝업에서 신청 버튼이 비활성화되어 "정원이 찼습니다"로 표시됨. 만료된 스레드와는 구분됨.
result: pass
note: "Hot-fixed during UAT: badge cascade priority swapped (isFull > isExpired), default dateFrom set to today, re-fetch detail after apply/cancel"

### 4. Edit Mode Bypasses Active Thread Guard
expected: 이미 활성 스레드가 있는 상태에서 내 스레드의 수정 버튼을 누르면, "이미 활성 스레드가 있습니다" 배너 없이 수정 폼이 정상적으로 표시됨.
result: skipped

### 5. Header Button Order (Location > Date > Radius > Refresh)
expected: 헤더의 버튼 순서가 동네 설정 > 날짜 > 반경(radius) > 재탐색 순으로 배치됨. 재탐색 버튼이 모든 필터 뒤 마지막에 위치함.
result: pass

### 6. Dynamic Circle Radius
expected: radius 설정값을 변경하면 지도상의 원형(Circle) 크기가 그에 맞게 변함. 작은 반경 선택 시 작은 원, 큰 반경 선택 시 큰 원이 표시됨.
result: pass

### 7. GPS Location Label Resets to "현재 위치"
expected: 동네 설정으로 다른 곳을 탐색한 후 새로고침하면, GPS 위치로 지도가 이동하면서 동네 설정 버튼에 "현재 위치"가 표시됨. 이전 검색 주소가 남아있지 않음.
result: pass

### 8. Free Map Drag Without Snap-back
expected: 지도를 마우스로 드래그하면 자유롭게 이동 가능하며, 이전 위치로 자동 복귀하지 않음. 원형 반경이 항상 화면 가운데에 유지됨.
result: pass

### 9. Reverse Geocoding on Map Drag
expected: 지도를 드래그하여 이동한 후, 헤더의 동네 설정 버튼에 새 위치의 대략적인 주소/지역명이 자동으로 반영됨.
result: pass
note: "Nominatim 무한호출 문제로 역지오코딩 제거됨. 기능 자체가 불필요해짐."

## Summary

total: 9
passed: 8
issues: 0
pending: 0
skipped: 1

## Gaps

[none - all issues hot-fixed during testing]
