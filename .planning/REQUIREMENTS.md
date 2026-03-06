# Requirements: Aini-inu Frontend Realignment

**Defined:** 2026-03-06
**Core Value:** 프론트엔드의 모든 API 호출이 백엔드 Swagger 명세와 100% 일치하고, PRD 요구사항이 빠짐없이 구현되어 런타임 에러 0건을 달성하는 것.

## v1 Requirements

Requirements for frontend realignment. Each maps to roadmap phases.

### Critical Bugs (Phase 1)

- [x] **BUG-01**: 전체 프론트엔드 런타임 에러 전수 조사 및 분류
- [x] **BUG-02**: 크리티컬 런타임 에러 즉시 수정 (페이지 크래시, 무한 루프, 렌더링 실패)
- [x] **BUG-03**: API 호출 불일치로 인한 네트워크 에러 수정 (잘못된 URL/method/payload)

### Common Infrastructure (Phase 2)

- [x] **INFRA-01**: API 레이어 중앙화 — 모든 도메인 API 호출을 `api/` 폴더 내 서비스 모듈로 통합
- [x] **INFRA-02**: ApiResponse<T> 봉투 패턴 공통 처리 — `{success, status, data, errorCode, message}` 구조 파싱
- [x] **INFRA-03**: 공통 에러 핸들링 — 에러 코드별 사용자 메시지 매핑, toast 정책 통일 (PRD §11.3)
- [x] **INFRA-04**: 페이지네이션 공통 처리 — SliceResponse/CursorResponse/PageResponse 타입 및 훅
- [x] **INFRA-05**: presigned URL 기반 이미지 업로드 유틸 공통화 (`UPLOAD-PRESIGNED-POST` → `UPLOAD-PRESIGNED-PUT` → `IMAGE-LOCAL-GET`)
- [x] **INFRA-06**: 인증 인터셉터 — JWT Bearer 토큰 자동 첨부, 401 시 리프레시, 만료 시 로그아웃 전이
- [x] **INFRA-07**: 5종 상태 패턴 공통 컴포넌트 — default/loading/empty/error/success (PRD §10.2)

### Authentication (Phase 3) — FR-AUTH

- [x] **AUTH-01**: 이메일 로그인 (FR-AUTH-001, `AUTH-LOGIN`)
- [x] **AUTH-02**: 회원가입 3단계 플로우 — Account → Profile → Pet (FR-AUTH-002, `AUTH-SIGNUP` → `MEM-PROFILE-CREATE` → `PET-CREATE`)
- [x] **AUTH-03**: 리프레시 토큰 갱신 (FR-AUTH-003, `AUTH-REFRESH`)
- [x] **AUTH-04**: 로그아웃 — 리프레시 토큰 폐기 (FR-AUTH-004, `AUTH-LOGOUT`)
- [x] **AUTH-05**: 가입 폼 검증 — 이메일 형식, 비밀번호 강도(대/소문자/숫자/특수문자), 닉네임 2~10자 (PRD §9.2)
- [x] **AUTH-06**: 가입 단계별 진행 조건 충족 시에만 다음 단계 활성화 (PRD §8.3)

### Member Profile/Relations (Phase 4) — FR-MEMBER

- [x] **MEM-01**: 내 프로필 조회 (FR-MEMBER-001, `MEM-ME-GET`)
- [x] **MEM-02**: 내 프로필 수정 (FR-MEMBER-001, `MEM-ME-PATCH`)
- [x] **MEM-03**: 타 회원 프로필 조회 (FR-MEMBER-002, `MEM-ID-GET`)
- [x] **MEM-04**: 타 회원 반려견 목록 조회 (FR-MEMBER-002, `MEM-ID-PETS-GET`)
- [x] **MEM-05**: 팔로워 목록 조회 (FR-MEMBER-003, `MEM-FOLLOWERS-GET`)
- [x] **MEM-06**: 팔로잉 목록 조회 (FR-MEMBER-003, `MEM-FOLLOWING-GET`)
- [x] **MEM-07**: 팔로우 (FR-MEMBER-004, `MEM-FOLLOWS-POST`)
- [x] **MEM-08**: 언팔로우 (FR-MEMBER-004, `MEM-FOLLOWS-DELETE`)
- [x] **MEM-09**: 산책 활동 통계 조회 (FR-MEMBER-005, `MEM-WALK-STATS-GET`)
- [x] **MEM-10**: 회원 검색 (FR-MEMBER-006, `MEM-SEARCH-GET`)
- [x] **MEM-11**: 회원 성향 마스터 조회 (FR-MEMBER-007, `MEM-PERSONALITY-TYPES-GET`)
- [x] **MEM-12**: 프로필 UI — 권한별 편집 가능 여부, 팔로우 토글 실패 복구, 반려견 빈 상태 (PRD §8.3)
- [x] **MEM-13**: 팔로우 카운트 + 목록 공개 (DEC-010)

### Pet Management (Phase 5) — FR-PET

- [x] **PET-01**: 반려견 등록 — `birthDate` canonical 단일 입력 (FR-PET-001, `PET-CREATE`, DEC-003)
- [x] **PET-02**: 반려견 수정 (FR-PET-001, `PET-PATCH`)
- [x] **PET-03**: 반려견 삭제 (FR-PET-001, `PET-DELETE`)
- [x] **PET-04**: 메인 반려견 변경 (FR-PET-002, `PET-MAIN-PATCH`)
- [x] **PET-05**: 견종 마스터 조회 (FR-PET-003, `PET-BREEDS-GET`)
- [x] **PET-06**: 성향 마스터 조회 (FR-PET-003, `PET-PERSONALITIES-GET`)
- [x] **PET-07**: 산책스타일 마스터 조회 (FR-PET-003, `PET-WALKING-STYLES-GET`)
- [x] **PET-08**: 반려견 이름 최대 10자, 회원당 최대 10마리 제한 (PRD §8.1)

### Walk Threads (Phase 6) — FR-WALK (모집/탐색)

- [x] **WALK-01**: 스레드 생성 — 비애견인 생성 불가 (FR-WALK-001/DEC-008, `THR-CREATE`)
- [x] **WALK-02**: 스레드 수정 (FR-WALK-001, `THR-PATCH`)
- [x] **WALK-03**: 스레드 삭제 (FR-WALK-001, `THR-DELETE`)
- [x] **WALK-04**: 스레드 목록 조회 (FR-WALK-001, `THR-LIST`)
- [x] **WALK-05**: 스레드 상세 조회 (FR-WALK-001, `THR-DETAIL`)
- [x] **WALK-06**: 스레드 신청 — 즉시 입장, 정원 초과 즉시 실패, 중복 멱등 (FR-WALK-002/DEC-012/013/014, `THR-APPLY-POST`)
- [x] **WALK-07**: 스레드 신청 취소 (FR-WALK-002, `THR-APPLY-DELETE`)
- [x] **WALK-08**: 지도 탐색 (FR-WALK-003, `THR-MAP-GET`)
- [x] **WALK-09**: 핫스팟 조회 (FR-WALK-003, `THR-HOTSPOT-GET`)
- [x] **WALK-10**: 스레드 생성 시 채팅 타입 필수 선택 — INDIVIDUAL/GROUP (FR-WALK-005/DEC-009)
- [x] **WALK-11**: `/around-me` 진입 시 GPS 1회 자동 획득, 실패 시 서울시청 fallback (FR-WALK-007/DEC-026/027/030)
- [x] **WALK-12**: 사용자 수동 재탐색만 허용, 자동 주기 갱신 없음 (FR-WALK-008/DEC-029)
- [x] **WALK-13**: 스레드 제목 최대 30자, 소개 최대 500자, 자동 만료 60분 (PRD §8.1/DEC-020)
- [x] **WALK-14**: 모집 작성 시 제목/시간/참여 반려견 필수 (PRD §9.2)

### Walk Diary + Story (Phase 7) — FR-WALK (일기) + FR-COMMUNITY (스토리)

- [x] **DIARY-01**: 산책일기 생성 — content 최대 300자, 기본 공개 (FR-WALK-004/006, `DIARY-CREATE`, DEC-011)
- [x] **DIARY-02**: 산책일기 목록 조회 (FR-WALK-004, `DIARY-LIST`)
- [x] **DIARY-03**: 산책일기 상세 조회 (FR-WALK-004, `DIARY-DETAIL`)
- [x] **DIARY-04**: 산책일기 수정 — content 최대 300자 (FR-WALK-004, `DIARY-PATCH`)
- [x] **DIARY-05**: 산책일기 삭제 (FR-WALK-004, `DIARY-DELETE`)
- [x] **DIARY-06**: 팔로잉 피드 (산책일기 팔로잉 목록) (FR-WALK-004, `DIARY-FOLLOWING-LIST`)
- [x] **DIARY-07**: 스토리 조회 — 팔로워 대상, 회원당 아이콘 1개 그룹, 24h 만료 (FR-COMMUNITY-004/DEC-022~025, `STORY-LIST`)

### Chat System (Phase 8) — FR-CHAT

- [ ] **CHAT-01**: 채팅방 목록 조회 — loading/empty/error 상태 (FR-CHAT-001, `CHAT-ROOMS-GET`)
- [ ] **CHAT-02**: 채팅방 상세 조회 (FR-CHAT-001, `CHAT-ROOM-GET`)
- [ ] **CHAT-03**: 1:1 채팅방 시작 — 기존 방 재사용 (FR-CHAT-001A, `CHAT-ROOM-DIRECT-CREATE`)
- [ ] **CHAT-04**: 메시지 커서 기반 조회 — 최신 우선, 과거 역방향 (FR-CHAT-002/DEC-017, `CHAT-MSG-LIST`)
- [ ] **CHAT-05**: 메시지 전송 (FR-CHAT-002, `CHAT-MSG-SEND`)
- [ ] **CHAT-06**: 메시지 상태 실시간 반영 — WebSocket created/delivered/read (FR-CHAT-002A/DEC-021, `CHAT-WS-EVENTS`)
- [ ] **CHAT-07**: 전송 실패 버블 재시도 (FR-CHAT-003/DEC-015, `CHAT-MSG-SEND`)
- [ ] **CHAT-08**: 채팅방 나가기 (FR-CHAT-004, `CHAT-LEAVE`)
- [ ] **CHAT-09**: 산책 확정 (FR-CHAT-004, `CHAT-WALK-CONFIRM-*`)
- [ ] **CHAT-10**: 후기 작성 — 채팅방/대상 1회, 수정 불가 (FR-CHAT-004/DEC-007, `CHAT-REVIEW-CREATE`)
- [ ] **CHAT-11**: 내 후기 조회 (FR-CHAT-004, `CHAT-REVIEW-ME`)
- [ ] **CHAT-12**: WebSocket STOMP 연결 — `/ws/chat-rooms/{roomId}` JWT 인증 (DEC-021)
- [ ] **CHAT-13**: 채팅 메시지 최대 500자, 폴링 5초 fallback (PRD §8.1/DEC-016)
- [ ] **CHAT-14**: 그룹 채팅 정원 3~10명 (PRD §8.1)

### Community Feed (Phase 9) — FR-COMMUNITY

- [ ] **FEED-01**: 게시글 생성 — 이미지/본문 필수 (FR-COMMUNITY-001/PRD §9.2, `POST-CREATE`)
- [ ] **FEED-02**: 게시글 목록 조회 (FR-COMMUNITY-001, `POST-LIST`)
- [ ] **FEED-03**: 게시글 상세 조회 (FR-COMMUNITY-001, `POST-DETAIL`)
- [ ] **FEED-04**: 게시글 수정 — content 필수 (FR-COMMUNITY-001/DEC-004, `POST-PATCH`)
- [ ] **FEED-05**: 게시글 삭제 (FR-COMMUNITY-001, `POST-DELETE`)
- [ ] **FEED-06**: 댓글 CRUD — 삭제 권한: 댓글 작성자 또는 게시글 작성자 (FR-COMMUNITY-002/DEC-019, `POST-COMMENT-*`)
- [ ] **FEED-07**: 좋아요 — 낙관적 반영 + 실패 롤백 (FR-COMMUNITY-002/DEC-018, `POST-LIKE-POST`)
- [ ] **FEED-08**: presigned URL 이미지 업로드 (FR-COMMUNITY-003, `UPLOAD-PRESIGNED-POST/PUT`)

### Lost Pet (Phase 10) — FR-LOST

- [ ] **LOST-01**: 실종 등록 (FR-LOST-001, `LOST-CREATE`)
- [ ] **LOST-02**: 제보 등록 (FR-LOST-001, `SIGHTING-CREATE`)
- [ ] **LOST-03**: AI 분석 — 유사 제보 후보 세션 스냅샷 조회 (FR-LOST-002, `LOST-ANALYZE`)
- [ ] **LOST-04**: 매칭 조회 — 재진입 순서 고정 (FR-LOST-002, `LOST-MATCH`)
- [ ] **LOST-05**: 매칭 승인 → 채팅 연결 (FR-LOST-003/DEC-006, `LOST-MATCH-APPROVE`)
- [ ] **LOST-06**: 분석 실패 처리 — 500 + L500_AI_ANALYZE_FAILED, 세션 미생성 (FR-LOST-004/DEC-005)
- [ ] **LOST-07**: 긴급 제보 UI — around-me 제보 탭, 이미지 분석 플로우 (PRD §8.3)

### Dashboard (Phase 11) — FR-DASH

- [ ] **DASH-01**: 인사/매너 점수/활동량 표시 (FR-DASH-001, `MEM-ME-GET` + `MEM-WALK-STATS-GET`)
- [ ] **DASH-02**: 산책 추천 카드 (FR-DASH-002, `THR-HOTSPOT-GET`)
- [ ] **DASH-03**: 동네 최신 스레드 요약 (FR-DASH-003, `THR-LIST`)
- [ ] **DASH-04**: 미작성 리뷰 모달 — 제출/실패 재시도 (FR-DASH-004, `CHAT-ROOMS-GET` + `CHAT-REVIEW-CREATE`)
- [ ] **DASH-05**: 섹션별 부분 실패 fallback (PRD §8.3)

### Settings + Integration (Phase 12) — FR-SET

- [ ] **SET-01**: 라이트/다크 테마 토글 (FR-SET-001, Local State)
- [ ] **SET-02**: 로그아웃 성공/실패 처리 (PRD §8.3, `AUTH-LOGOUT`)
- [ ] **SET-03**: 전체 통합 UAT — 73개 엔드포인트 Swagger 일치 검증
- [ ] **SET-04**: PRD §8 FR 34개 요구사항 커버리지 최종 검증
- [ ] **SET-05**: DEC 31개 정책 잠금값 준수 최종 검증
- [ ] **SET-06**: 런타임 에러 0건 최종 확인
- [ ] **SET-07**: agent-browser UAT 스크린샷 증빙 확보

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Notifications (PRD §4.3 Phase2)

- **NOTF-01**: 인앱 알림 시스템
- **NOTF-02**: 푸시 알림 (Phase2 적용 예정)

### MVP Improvements (PRD §13)

- **IMP-01**: 레이더 마커 상호작용 개선
- **IMP-02**: 컴포넌트 mode/status 리팩터링
- **IMP-03**: 일기/피드 편집 플로우 일관성 강화

## Out of Scope

| Feature | Reason |
|---------|--------|
| 백엔드 코드 수정 | 읽기 전용 — API 계약은 백엔드가 진실의 원천 |
| common-docs 수정 | 읽기 전용 — PRD/OpenAPI는 고정 |
| 새 API 엔드포인트 추가 | 기존 73개 명세 내에서만 작업 |
| SSR/RSC 전환 | 현재 'use client' 패턴 유지 |
| 모바일 앱 | 웹 프론트엔드만 대상 |
| CI/CD 파이프라인 | 이번 범위 아님 |
| 알림 기능 | PRD §4.3 Phase2로 명시 연기 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| BUG-01 | Phase 1 | Complete |
| BUG-02 | Phase 1 | Complete |
| BUG-03 | Phase 1 | Pending |
| INFRA-01 | Phase 2 | Complete |
| INFRA-02 | Phase 2 | Complete |
| INFRA-03 | Phase 2 | Complete |
| INFRA-04 | Phase 2 | Complete |
| INFRA-05 | Phase 2 | Complete |
| INFRA-06 | Phase 2 | Complete |
| INFRA-07 | Phase 2 | Complete |
| AUTH-01 | Phase 3 | Complete |
| AUTH-02 | Phase 3 | Complete |
| AUTH-03 | Phase 3 | Complete |
| AUTH-04 | Phase 3 | Complete |
| AUTH-05 | Phase 3 | Complete |
| AUTH-06 | Phase 3 | Complete |
| MEM-01 | Phase 4 | Complete |
| MEM-02 | Phase 4 | Complete |
| MEM-03 | Phase 4 | Complete |
| MEM-04 | Phase 4 | Complete |
| MEM-05 | Phase 4 | Complete |
| MEM-06 | Phase 4 | Complete |
| MEM-07 | Phase 4 | Complete |
| MEM-08 | Phase 4 | Complete |
| MEM-09 | Phase 4 | Complete |
| MEM-10 | Phase 4 | Complete |
| MEM-11 | Phase 4 | Complete |
| MEM-12 | Phase 4 | Complete |
| MEM-13 | Phase 4 | Complete |
| PET-01 | Phase 5 | Complete |
| PET-02 | Phase 5 | Complete |
| PET-03 | Phase 5 | Complete |
| PET-04 | Phase 5 | Complete |
| PET-05 | Phase 5 | Complete |
| PET-06 | Phase 5 | Complete |
| PET-07 | Phase 5 | Complete |
| PET-08 | Phase 5 | Complete |
| WALK-01 | Phase 6 | Complete |
| WALK-02 | Phase 6 | Complete |
| WALK-03 | Phase 6 | Complete |
| WALK-04 | Phase 6 | Complete |
| WALK-05 | Phase 6 | Complete |
| WALK-06 | Phase 6 | Complete |
| WALK-07 | Phase 6 | Complete |
| WALK-08 | Phase 6 | Complete |
| WALK-09 | Phase 6 | Complete |
| WALK-10 | Phase 6 | Complete |
| WALK-11 | Phase 6 | Complete |
| WALK-12 | Phase 6 | Complete |
| WALK-13 | Phase 6 | Complete |
| WALK-14 | Phase 6 | Complete |
| DIARY-01 | Phase 7 | Complete |
| DIARY-02 | Phase 7 | Complete |
| DIARY-03 | Phase 7 | Complete |
| DIARY-04 | Phase 7 | Complete |
| DIARY-05 | Phase 7 | Complete |
| DIARY-06 | Phase 7 | Complete |
| DIARY-07 | Phase 7 | Complete |
| CHAT-01 | Phase 8 | Pending |
| CHAT-02 | Phase 8 | Pending |
| CHAT-03 | Phase 8 | Pending |
| CHAT-04 | Phase 8 | Pending |
| CHAT-05 | Phase 8 | Pending |
| CHAT-06 | Phase 8 | Pending |
| CHAT-07 | Phase 8 | Pending |
| CHAT-08 | Phase 8 | Pending |
| CHAT-09 | Phase 8 | Pending |
| CHAT-10 | Phase 8 | Pending |
| CHAT-11 | Phase 8 | Pending |
| CHAT-12 | Phase 8 | Pending |
| CHAT-13 | Phase 8 | Pending |
| CHAT-14 | Phase 8 | Pending |
| FEED-01 | Phase 9 | Pending |
| FEED-02 | Phase 9 | Pending |
| FEED-03 | Phase 9 | Pending |
| FEED-04 | Phase 9 | Pending |
| FEED-05 | Phase 9 | Pending |
| FEED-06 | Phase 9 | Pending |
| FEED-07 | Phase 9 | Pending |
| FEED-08 | Phase 9 | Pending |
| LOST-01 | Phase 10 | Pending |
| LOST-02 | Phase 10 | Pending |
| LOST-03 | Phase 10 | Pending |
| LOST-04 | Phase 10 | Pending |
| LOST-05 | Phase 10 | Pending |
| LOST-06 | Phase 10 | Pending |
| LOST-07 | Phase 10 | Pending |
| DASH-01 | Phase 11 | Pending |
| DASH-02 | Phase 11 | Pending |
| DASH-03 | Phase 11 | Pending |
| DASH-04 | Phase 11 | Pending |
| DASH-05 | Phase 11 | Pending |
| SET-01 | Phase 12 | Pending |
| SET-02 | Phase 12 | Pending |
| SET-03 | Phase 12 | Pending |
| SET-04 | Phase 12 | Pending |
| SET-05 | Phase 12 | Pending |
| SET-06 | Phase 12 | Pending |
| SET-07 | Phase 12 | Pending |

**Coverage:**
- v1 requirements: 96 total
- Mapped to phases: 96
- Unmapped: 0

---
*Requirements defined: 2026-03-06*
*Last updated: 2026-03-06 after initial definition*
