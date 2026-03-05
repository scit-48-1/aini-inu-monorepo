# AINI INU PROJECT PRD

## 0. 문서 메타
- 문서명: AINI INU Project PRD
- 버전: v1.3
- 작성일: 2026-02-26
- 목적: 서비스 정체성, 기능 범위, UX 원칙, 품질 기준과 문서 거버넌스를 단일 문서로 고정
- 이번 라운드 범위: 모든 도메인 기능 (운영/관측/롤아웃 제외)

### 0.1 문서 거버넌스 (최상위 잠금)
- 요구사항/정책/계약/모델의 1차 원본은 `common-docs`이며, 백엔드 코드는 이를 구현한다.
- API 계약의 단일 원본은 Swagger(OpenAPI)이며, REST + 봉투패턴(`ApiResponse<T>`)을 강제한다.
- 데이터/모델의 최종 구현 기준은 Backend JPA Entity와 `db/ddl/*.sql`이며, API 필드와 매핑 가능한 상태를 유지한다.
- 구현 갭은 GitHub Issue/PR 단위로 추적한다.

### 0.2 문서 책임 맵 (Decision Complete)
| 문서 | 책임 | 구현체 반영 대상 |
|---|---|---|
| `PROJECT_PRD.md` | 제품 범위, 정책 잠금(DEC), 문서 거버넌스 | Backend 정책 분기/권한/제약 |
| `Swagger(OpenAPI)` | 경로/메서드/요청/응답 계약 | BE Controller/DTO + Swagger 동기화 |

## 1. 제품 정체성
### 1.1 비전
반려견 산책의 모든 순간을 연결하는 지역 기반 신뢰 커뮤니티를 만든다.

### 1.2 미션
사용자가 동네에서 안전하고 즐겁게 산책 메이트를 찾고, 관계를 쌓고, 기록을 남기고, 긴급 상황까지 해결하도록 돕는다.

### 1.3 제품 약속
- 빠른 발견: 지금 내 주변에서 바로 연결되는 산책 레이더 경험 제공
- 신뢰 기반 관계: 매너 점수, 후기, 프로필 맥락으로 관계 품질 확보
- 기록의 지속성: 피드와 산책일기로 경험을 축적
- 안전 우선: 실종/발견 제보와 매칭으로 긴급 상황 대응

### 1.4 브랜드 메시지
`산책의 진심을 잇다.`

## 2. 문제 정의
### 2.1 핵심 문제
- 동네에서 산책 메이트를 찾는 과정이 비효율적이다.
- 연결 전 상대 신뢰를 판단할 정보가 부족하다.
- 산책 경험이 일회성으로 끝나고 누적되지 않는다.
- 실종/발견 상황에서 즉시 대응 가능한 지역 네트워크가 약하다.

### 2.2 해결 전략
- 지도 중심 실시간 스레드 탐색
- 채팅/후기/매너 점수 기반 관계 품질 관리
- 피드/산책일기 기반 경험 축적 + 스토리(산책일기 기반 24시간 하이라이트)
- 이미지 분석 + 후보 매칭 기반 긴급 대응

## 3. 타겟 사용자
### 3.1 Primary Persona
- 반려견 보호자 (20-40대, 도심 거주)
- 니즈: 산책 동행, 사회화, 안전한 연결, 일상 기록

### 3.2 Secondary Persona
- 비보호자 이웃/관심 사용자
- 니즈: 동네 커뮤니티 참여, 반려 문화 교류

### 3.3 Emergency Persona
- 실종 반려견 보호자, 유기견 발견 제보자
- 니즈: 빠른 제보 접수, 후보 탐색, 즉시 연락

## 4. 제품 범위
### 4.1 In Scope
- 인증/회원가입/프로필
- 대시보드
- 동네 탐색(찾기/모집/긴급제보)
- 채팅(목록/대화/프로필 탐색/후기)
- 피드(게시글/댓글/스토리/일기 뷰어)
- 프로필(내/타인, 팔로우, 반려견, 산책일기)
- 설정(테마/로그아웃)

### 4.2 도메인 용어 잠금 (Story vs WalkDiary)
| 용어 | 역할 | 지속성 | 노출 대상 | 정렬/만료 기준 |
|---|---|---|---|---|
| 산책일기(WalkDiary) | 산책 경험의 원본 기록 객체(CRUD 대상) | 영구 보관(삭제 전까지) | 작성자 본인/공개 범위에 따른 타 사용자 | 목록 정렬은 API 계약 기준, 만료 없음 |
| 스토리(Story) | 산책일기에서 파생되는 임시 노출 뷰(조회 전용) | 임시 노출(24시간) | 작성자를 팔로우한 사용자 | `diary.createdAt` 최신순, `createdAt + 24h` 만료 |

- 스토리는 독립 작성/수정/삭제 도메인이 아니며, `STORY-LIST`는 산책일기 파생 결과를 조회하는 API다.
- 동일 회원이 24시간 내 산책일기를 여러 개 작성하면, 피드에서는 회원당 스토리 아이콘 1개로 묶고 진입 후 순차 노출한다.
- 산책일기의 공개범위 변경/삭제/수정은 스토리 노출 결과에 즉시 반영한다.

### 4.3 단계별 적용 범위
- Phase1: 설정 범위는 테마 토글과 로그아웃만 포함한다.
- Phase2: 알림 기능(푸시 알림 포함)을 적용한다.
- 알림 기능은 Phase2에서 적용하며, 현재 Phase1 계약 범위에서는 제외한다.

## 6. 서비스 정보구조(IA)
### 6.1 주요 라우트
- `/` 랜딩
- `/login` 로그인
- `/signup` 다단계 가입
- `/dashboard` 개인 허브
- `/around-me` 동네 탐색/모집/제보
- `/chat` 채팅 목록
- `/chat/[id]` 채팅 상세
- `/feed` 커뮤니티 피드
- `/profile/[memberId]` 프로필
- `/settings` 설정

### 6.2 글로벌 내비게이션 원칙
- 데스크톱: 좌측 고정 사이드바 + 주요 섹션 진입
- 모바일: 하단 탭바 중심 진입
- 모든 코어 기능은 2탭 이내 도달 가능해야 함

## 7. 핵심 사용자 여정
### 7.1 신규 사용자
랜딩 -> 회원가입(계정/반려견/보호자) -> 대시보드 -> 동네 탐색 첫 참여

### 7.2 활성 사용자
대시보드 -> around-me에서 스레드 참여 -> 채팅 -> 후기 작성 -> 피드/프로필 기록

### 7.3 긴급 사용자
around-me 제보 탭 -> 이미지 분석 -> 후보 매칭 -> 채팅 연결/후속 처리

## 8. 기능 요구사항 (Functional Requirements)
본 문서가 기능 요구사항(FR)의 단일 원본이며, API 경로/스키마의 최종 계약은 Swagger(OpenAPI)를 따른다.

### 8.1 전역 제약 (FR-GLOBAL)
| 항목 | 제약 |
|---|---|
| 닉네임 | 2~10자 |
| 반려견 이름 | 최대 10자 |
| 스레드 제목 | 최대 30자 |
| 스레드 소개 | 최대 500자 |
| 산책일기 `content` | 최대 300자 (스토리 파생 원본 동일 제약 적용) |
| 채팅 메시지 | 최대 500자 |
| 반려견 등록 수 | 회원당 최대 10마리 |
| 그룹 채팅 정원 | 3~10명 |
| 레이더 기본 탐색 반경 | 5km |
| 레이더 갱신 방식 | `/around-me` 진입 시 초기 1회 위치 획득 후 사용자 수동 갱신(자동 주기 없음) |
| 스레드 자동 만료 | 시작시간 기준 60분 경과 |
| 채팅 메시지 폴링 주기 | 5초 |
| 후기 별점 범위 | 1~5 |
| 후기 작성 횟수 | 채팅방/작성대상 회원 기준 1회(동일 작성자가 같은 상대에게는 1회, 다른 상대에게는 작성 가능; 작성 후 수정/재작성 불가) |

### 8.2 도메인 FR 매트릭스
#### 8.2.1 인증/회원 (FR-AUTH)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-AUTH-001 | 이메일 로그인 지원 | `AUTH-LOGIN` |
| FR-AUTH-002 | 회원가입 후 프로필 완료 흐름 제공 | `AUTH-SIGNUP`, `MEM-PROFILE-CREATE` |
| FR-AUTH-003 | 리프레시 토큰 갱신 | `AUTH-REFRESH` |
| FR-AUTH-004 | 로그아웃(리프레시 토큰 폐기) | `AUTH-LOGOUT` |

#### 8.2.2 대시보드 (FR-DASH)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-DASH-001 | 개인 인사/매너 점수/활동량 표시 | `MEM-ME-GET`, `MEM-WALK-STATS-GET` |
| FR-DASH-002 | 산책 추천 카드 제공 | `THR-HOTSPOT-GET` |
| FR-DASH-003 | 동네 최신 스레드 요약 제공 | `THR-LIST` |
| FR-DASH-004 | 미작성 리뷰가 있으면 리뷰 모달 노출 | `CHAT-ROOMS-GET`, `CHAT-REVIEW-CREATE` |

#### 8.2.3 회원 프로필/관계 (FR-MEMBER)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-MEMBER-001 | 내 프로필 조회/수정 | `MEM-ME-GET`, `MEM-ME-PATCH` |
| FR-MEMBER-002 | 타 회원 프로필/반려견 조회 | `MEM-ID-GET`, `MEM-ID-PETS-GET` |
| FR-MEMBER-003 | 팔로워/팔로잉 조회 | `MEM-FOLLOWERS-GET`, `MEM-FOLLOWING-GET` |
| FR-MEMBER-004 | 팔로우/언팔로우 (복수형 리소스) | `MEM-FOLLOWS-POST`, `MEM-FOLLOWS-DELETE` |
| FR-MEMBER-005 | 산책 활동 통계 조회 | `MEM-WALK-STATS-GET` |
| FR-MEMBER-006 | 회원 검색(채팅 시작/이웃 탐색) | `MEM-SEARCH-GET` |
| FR-MEMBER-007 | 회원 성향 마스터 조회/선택(회원가입/프로필 편집) | `MEM-PERSONALITY-TYPES-GET`, `MEM-PROFILE-CREATE`, `MEM-ME-PATCH` |

#### 8.2.4 반려견 (FR-PET)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-PET-001 | 반려견 등록/수정/삭제 (`birthDate` canonical 단일 입력) | `PET-CREATE`, `PET-PATCH`, `PET-DELETE` |
| FR-PET-002 | 메인 반려견 변경 | `PET-MAIN-PATCH` |
| FR-PET-003 | 견종/성향/산책스타일 마스터 조회 | `PET-BREEDS-GET`, `PET-PERSONALITIES-GET`, `PET-WALKING-STYLES-GET` |

#### 8.2.5 산책 모집/일기 (FR-WALK)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-WALK-001 | 스레드 생성/수정/삭제/조회 (비애견인 생성 불가) | `THR-CREATE`, `THR-PATCH`, `THR-DELETE`, `THR-LIST`, `THR-DETAIL` |
| FR-WALK-002 | 스레드 신청/취소 (비애견인 신청 허용, 즉시 입장, 정원 초과 즉시 실패, 중복 신청 멱등 성공) | `THR-APPLY-POST`, `THR-APPLY-DELETE` |
| FR-WALK-003 | 지도/핫스팟 탐색 | `THR-MAP-GET`, `THR-HOTSPOT-GET` |
| FR-WALK-004 | 산책일기 CRUD 및 팔로잉 피드 (스토리 파생 원본 유지: 산책일기 자체는 영구 기록, `content` 최대 300자) | `DIARY-CREATE`, `DIARY-LIST`, `DIARY-DETAIL`, `DIARY-PATCH`, `DIARY-DELETE`, `DIARY-FOLLOWING-LIST` |
| FR-WALK-005 | 스레드 생성 시 채팅 타입(`INDIVIDUAL`/`GROUP`) 필수 지정 | `THR-CREATE` |
| FR-WALK-006 | 산책일기 공개 범위 기본값 공개 + 비공개 선택 (변경/삭제/수정은 스토리 노출에 즉시 반영) | `DIARY-CREATE`, `DIARY-PATCH` |
| FR-WALK-007 | `/around-me` 진입 시 프론트엔드는 JavaScript `navigator.geolocation.getCurrentPosition()`을 1회 호출해 현재 좌표를 획득한다. 권한 거부/타임아웃/브라우저 미지원 시 서비스 기본좌표(서울시청)로 fallback한다. | `THR-LIST`, `THR-MAP-GET`, `THR-HOTSPOT-GET` |
| FR-WALK-008 | 프론트엔드는 사용자의 명시적 액션(현재 위치 재탐색)에서만 `getCurrentPosition()` 재호출과 주변 데이터(`THR-LIST`, `THR-MAP-GET`, `THR-HOTSPOT-GET`) 재조회를 수행한다. 자동 주기 재호출/재조회는 수행하지 않는다. | `THR-LIST`, `THR-MAP-GET`, `THR-HOTSPOT-GET` |

#### 8.2.6 채팅 (FR-CHAT)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-CHAT-001 | 채팅방 목록/상세 조회(실데이터, loading/empty/error 상태 포함) | `CHAT-ROOMS-GET`, `CHAT-ROOM-GET` |
| FR-CHAT-001A | 팔로잉/회원검색에서 1:1 채팅방 시작(기존 방이 있으면 재사용) | `CHAT-ROOM-DIRECT-CREATE` |
| FR-CHAT-002 | 메시지 커서 기반 조회/전송(최신 우선, 과거 역방향 페이징) | `CHAT-MSG-LIST`, `CHAT-MSG-SEND` |
| FR-CHAT-002A | 메시지 상태(전송완료/읽음) 실시간 반영 | `CHAT-WS-EVENTS` |
| FR-CHAT-003 | 전송 실패 메시지 버블 재시도 | `CHAT-MSG-SEND` |
| FR-CHAT-004 | 채팅방 나가기/산책 확정/후기(채팅방/작성대상 회원 기준 1회 작성, 동일 작성자는 같은 상대에게 중복 작성 불가, 작성 후 수정 불가) | `CHAT-LEAVE`, `CHAT-WALK-CONFIRM-*`, `CHAT-REVIEW-CREATE`, `CHAT-REVIEW-ME` |

#### 8.2.7 실종 반려견 (FR-LOST)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-LOST-001 | 실종 등록/제보 등록 | `LOST-CREATE`, `SIGHTING-CREATE` |
| FR-LOST-002 | 실종 기준 유사 제보 후보를 세션 스냅샷으로 조회(재진입 순서 고정) | `LOST-ANALYZE`, `LOST-MATCH` |
| FR-LOST-003 | 매칭 및 채팅 연결은 분석 세션 후보 검증 후 사용자 명시 승인으로 생성 | `LOST-MATCH`, `LOST-MATCH-APPROVE` |
| FR-LOST-004 | 긴급 제보 이미지 분석 보조(실패 시 500 + 도메인 에러코드 반환, fallback 미제공, 실패 시 세션 미생성) | `LOST-ANALYZE`, `LOST-CREATE`, `SIGHTING-CREATE` |

#### 8.2.8 커뮤니티/업로드 (FR-COMMUNITY)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-COMMUNITY-001 | 게시글 CRUD (생성/수정 모두 본문 필수, UI 라벨 caption 허용) | `POST-CREATE`, `POST-LIST`, `POST-DETAIL`, `POST-PATCH`, `POST-DELETE` |
| FR-COMMUNITY-002 | 댓글/좋아요/스토리 조회 (좋아요 낙관적 반영+실패 롤백, 댓글 삭제 권한 규칙 포함) | `POST-COMMENT-*`, `POST-LIKE-POST`, `STORY-LIST` |
| FR-COMMUNITY-003 | presigned URL 기반 로컬 이미지 업로드 (MVP) | `UPLOAD-PRESIGNED-POST`, `UPLOAD-PRESIGNED-PUT`, `IMAGE-LOCAL-GET` |
| FR-COMMUNITY-004 | 스토리 조회 정책: 작성자 팔로워 대상, 회원당 아이콘 1개 그룹, 그룹 내부는 24시간 내 산책일기 최신순 순차 노출, 만료 기준 `diary.createdAt + 24h` | `STORY-LIST`, `DIARY-LIST`, `DIARY-DETAIL` |

#### 8.2.9 설정 (FR-SET)
| ID | 요구사항 | API Ref |
|---|---|---|
| FR-SET-001 | 라이트/다크 테마 토글 제공 | `N/A (Local State)` |

### 8.3 UI/UX 플로우 커버리지 기준
- UI/UX 커버리지는 `핵심 플로우 + 상태`를 필수 기준으로 한다.
- 상태는 최소 `입력 검증`, `오류`, `빈 상태`, `권한/인증`, `성공 후 전이`를 포함해야 한다.

| 플로우 | 필수 상태 | API Ref |
|---|---|---|
| 회원가입(Account -> Profile -> Pet) | 비밀번호 규칙 검증, 중복/형식 오류, 단계 이동/완료 전이 | `AUTH-SIGNUP`, `MEM-PROFILE-CREATE`, `PET-CREATE` |
| 대시보드(요약/추천/리뷰) | 섹션별 부분 실패 fallback, 리뷰 모달 제출/실패 재시도 | `THR-LIST`, `THR-HOTSPOT-GET`, `CHAT-ROOMS-GET`, `CHAT-REVIEW-CREATE`, `MEM-WALK-STATS-GET` |
| 피드(스토리/게시글/댓글/좋아요) | 스토리 그룹(회원당 아이콘 1개), 그룹 내부 산책일기 순차 뷰어, 24시간 만료 처리, 산책일기 비공개/삭제 즉시 반영, 목록 빈 상태, 댓글 로딩/실패, 좋아요 낙관적 반영 실패 롤백 | `POST-LIST`, `POST-COMMENT-*`, `POST-LIKE-POST`, `STORY-LIST`, `DIARY-FOLLOWING-LIST`, `DIARY-LIST`, `DIARY-DETAIL` |
| 프로필(내/타인/관계) | 권한별 편집 가능 여부, 팔로우 토글 실패 복구, 반려견 목록 빈 상태, 메인 반려견 변경 | `MEM-ME-*`, `MEM-ID-*`, `MEM-FOLLOW*`, `MEM-WALK-STATS-GET`, `PET-MAIN-PATCH`, `MEM-PERSONALITY-TYPES-GET` |
| 주변탐색(찾기/모집/긴급) | `/around-me` 진입 시 GPS 1회 자동 획득 성공, 권한 거부/타임아웃/브라우저 미지원 시 서울시청 기본좌표(`lat=37.566295`, `lng=126.977945`) fallback, 사용자 수동 재탐색(자동 주기 갱신 없음), 신청/삭제 권한, 긴급 제보 분석 실패 시 `500 + L500_AI_ANALYZE_FAILED` 반환 및 세션 미생성, 세션 만료 시 재분석 전이 | `THR-*`, `LOST-ANALYZE`, `LOST-*`, `SIGHTING-*` |
| 채팅(목록/대화/후기) | 메시지 커서 페이징, 전송 실패 재시도, 산책 확정/후기 중복 처리 | `CHAT-*` |
| 설정(테마/로그아웃) | 테마 로컬 상태 저장, 로그아웃 성공/실패 처리 | `AUTH-LOGOUT` |

## 9. 폼/입력 요구사항
### 9.1 공통
- 모든 필수 입력값은 버튼 비활성 + 실시간 오류 메시지로 이중 방어
- 서버 오류는 사용자 행동 가능한 문구로 표준화
- 제출 중 상태는 CTA 로딩 및 중복 제출 방지

### 9.2 주요 검증
- 이메일 형식 검증
- 비밀번호 강도 검증 (대/소문자, 숫자, 특수문자 포함)
- 가입 단계별 진행 조건 충족 시에만 다음 단계 활성화
- 모집 작성 시 제목/시간/참여 반려견 필수
- 게시글 작성 시 이미지/본문 필수 (UI caption 입력란 사용 가능)
- 리뷰 작성 시 별점 필수
- 게시글 수정 시 본문(`content`) 필수 (UI `caption` 라벨은 허용)
- 산책일기 작성/수정 시 `content`는 최대 300자
- 반려견 입력은 `birthDate`를 canonical 원본으로 사용, `age` 입력은 허용하지 않음
- 스레드 신청은 정원 초과 시 즉시 실패, 중복 신청은 멱등 성공 처리

## 10. UI/UX 요구사항
### 10.1 경험 원칙
- 속도감: 핵심 행동까지 클릭 수 최소화
- 신뢰감: 프로필/매너/후기 정보를 행동 직전에 배치
- 안정감: 실패 상황에서도 복구 가능한 흐름 제공
- 감성: 기록 뷰어/카드/모달의 브랜드 무드 유지

### 10.2 인터랙션 원칙
- 상태는 최소 `default/loading/empty/error/success` 5종 보장
- 모달은 열고 닫힘, 포커스 복귀, 키보드 닫기 동작 보장
- 지도/카드/모달이 겹칠 때 우선순위 명확화

### 10.3 반응형 원칙
- 360px 이상에서 가로 스크롤/검은 여백이 없어야 함
- 모바일에서 핵심 CTA가 fold 아래로 밀리지 않도록 우선 배치
- 탭바/헤더/폼 간 충돌 없이 단일 스크롤 컨텍스트 유지

### 10.4 접근성 원칙
- 모든 입력에 label-id 연결 필수
- 키보드로 모든 주요 액션 수행 가능해야 함
- 명도 대비 WCAG AA 준수
- 아이콘 버튼은 aria-label 제공 필수

## 11. 기술/아키텍처 요구사항
### 11.1 백엔드 아키텍처
- Spring Boot 기반 컨텍스트 분리(member/pet/walk/chat/community/lostpet)
- Controller-Service-Repository 계층 분리와 트랜잭션 경계 명확화
- OpenAPI(Swagger) 계약을 실행 원본으로 유지

### 11.2 백엔드 설계 원칙
- DTO-도메인 모델 분리, 컨텍스트 간 직접 엔티티 참조 최소화
- 중복/충돌 정책은 에러코드 단위로 명시 분리
- SQL 초기화 정책(`db/ddl/*.sql`)과 Backend Entity/JPA 매핑 동기화 유지

### 11.3 에러 처리 원칙
- 사용자 액션 단위 toast 정책 통일
- 네트워크 오류, 권한 오류, 검증 오류를 구분된 메시지로 제공

## 12. 품질 요구사항 (Non-Functional Requirements)
- 핵심 액션 실패 시 사용자 재시도 경로를 제공한다.
- 폴링/비동기 업데이트는 중복 호출을 방지한다.
- 인증/프로필/위치/이미지 데이터 처리는 최소 권한 원칙을 적용한다.
- 위치 권한은 `/around-me` 진입 시점에만 요청하고, 획득 실패 시 서비스 기본좌표(서울시청 `lat=37.566295`, `lng=126.977945`)로 전이해 서비스 연속성을 보장한다.

## 13. MVP 권장 개선목록 (비필수)
- 레이더 마커 상호작용 개선
- 채팅 목록 실데이터 UX 완성
- 컴포넌트 mode/status 리팩터링
- 일기/피드 편집 플로우 일관성 강화

## 15. 정책 잠금 (v1.3)
| ID | 정책 | 잠금값 |
|---|---|---|
| DEC-001 | 문서 우선순위 | `PROJECT_PRD -> Swagger(OpenAPI) -> Backend 코드` |
| DEC-002 | 채팅 목록 정책 | placeholder 종료, API 실데이터 기반 상태 제공 |
| DEC-003 | 반려견 입력 정책 | `birthDate` canonical, `age` 입력 비허용 |
| DEC-004 | 게시글 수정 | `content` 필수 (`caption`은 UI 라벨/호환 alias) |
| DEC-005 | 긴급 분석 실패 | `500 + L500_AI_ANALYZE_FAILED` 반환, 수동 제보 fallback 제거, 실패 시 세션 미생성 |
| DEC-006 | 긴급 매칭 채팅 | 사용자 명시 승인 후 생성 |
| DEC-007 | 후기 정책 | 채팅방/작성대상 사용자 1회 작성(동일 작성자는 같은 상대에게만 1회), 작성 후 수정/재작성 불가 |
| DEC-008 | 비애견인 권한 | 스레드 생성 불가, 신청 가능 |
| DEC-009 | 스레드 채팅 타입 | 생성 시 필수 선택 (`INDIVIDUAL`/`GROUP`) |
| DEC-010 | 팔로우 공개 범위 | 카운트 + 목록 공개 |
| DEC-011 | 일기 공개 정책 | 기본 공개, 비공개 선택 가능 |
| DEC-012 | 신청 처리 방식 | 즉시 참여/채팅 입장 |
| DEC-013 | 정원 초과 처리 | 선착순, 즉시 실패 |
| DEC-014 | 중복 신청 처리 | 멱등 성공 |
| DEC-015 | 채팅 전송 실패 UX | 실패 버블에 재전송 버튼 |
| DEC-016 | 채팅 폴링 주기 | 5초 |
| DEC-017 | 채팅 페이징 | 최신 우선 + 과거 역방향 cursor |
| DEC-018 | 좋아요 처리 | 낙관적 반영 + 실패 롤백 |
| DEC-019 | 댓글 삭제 권한 | 댓글 작성자 또는 게시글 작성자 |
| DEC-020 | 레이더 기본값 | 반경 5km, 갱신은 사용자 수동(진입 시 초기 1회 위치 획득 이후 자동 주기 없음), 시작+60분 만료 |
| DEC-021 | 채팅 실시간 이벤트 | WebSocket(`created/delivered/read`) 우선, REST 폴링 fallback |
| DEC-022 | 스토리 도메인 성격 | 산책일기 파생 임시 노출 뷰(조회 전용, 독립 CRUD 없음) |
| DEC-023 | 스토리 노출 대상 | 작성자를 팔로우한 사용자 |
| DEC-024 | 스토리 정렬/만료 기준 | `diary.createdAt` 최신순, `createdAt + 24h` 만료 |
| DEC-025 | 스토리 그룹 UX | 회원당 아이콘 1개, 그룹 내부 다건 산책일기 순차 노출 |
| DEC-026 | around-me 위치 획득 API | 프론트엔드는 `navigator.geolocation.getCurrentPosition()`을 기본 호출로 사용 |
| DEC-027 | 위치 획득 실패 전이 | 권한 거부/타임아웃/브라우저 미지원 모두 동일하게 서울시청 기본좌표 fallback |
| DEC-028 | 위치 좌표 정밀도 | 백엔드 전송 `lat/lng`는 소수점 6자리 유지 |
| DEC-029 | 위치/레이더 재조회 방식 | 자동 주기 재호출/재조회 없음, 사용자 수동 재탐색만 허용 |
| DEC-030 | 서비스 기본좌표 | 서울시청 `lat=37.566295`, `lng=126.977945` 고정 |
| DEC-031 | 테스트 토큰 API 문서 노출 | FE 개발 편의를 위해 `POST /api/v1/test/auth/token`은 Swagger(OpenAPI)에 노출 유지 (`/api/v1/test/auth/me`는 제외 유지) |

### 15.1 충돌 해결 규칙
- 동일 주제에서 문서 충돌 시 본 문서의 잠금값(DEC)을 우선 적용한다.
- API 표현 충돌 시 Swagger(OpenAPI)를 API 단일 원본으로 사용한다.
- 단, 테스트 토큰 API(`POST /api/v1/test/auth/token`)는 DEC-031에 따라 개발 편의 목적의 예외 노출로 취급한다.
- Backend 코드와 문서가 충돌할 때는 문서를 우선으로 정렬하고, 문서 공백이 있으면 PRD 잠금값을 먼저 갱신한 뒤 구현을 반영한다.

### 15.2 구현 준비 체크
- [x] 사용자 권한/가드 규칙 잠금 완료
- [x] 상태전이/실패복구 규칙 잠금 완료
- [x] 길이/주기/반경 등 수치값 잠금 완료
- [x] PRD-API 동기화 기준 잠금 완료

## 16. 수용 기준 (Definition of Done)
- 기능: 핵심 MVP 기능이 문서 정의대로 동작한다.
- 계약: `PROJECT_PRD(§8 FR)`/Swagger(OpenAPI)와 구현(Backend Entity/`db/ddl/*.sql`)이 일치한다.
- 추적: Backend 미구현 API는 GitHub Issue/PR에 반드시 기록되어 있다.

## 17. 동기화 문서 (최상위 인덱스)
| 문서 | 역할 | 현재 기준 동기화 상태 |
|---|---|---|
| `Swagger(OpenAPI)` | API 계약 단일 원본 | Chat/Community/LostPet/WalkDiary 계약을 구현 코드 기준으로 동기화 (`CHAT-LEAVE` POST, `MyChatReviewResponse`, `LOST-ANALYZE` canonical 등) |
| `aini-inu-backend/src/main/java` + `src/main/resources/db/ddl/*.sql` | 데이터 모델/매핑 최종 구현 기준 | JPA Entity + DDL 인덱스/유니크 정책 + `db/ddl/*.sql` 자동 적용 정책(Flyway 미사용) 동기화 |

## 18. Backend 구현 정합성 스냅샷 (2026-03-01)
| 도메인 | Contract Endpoints | Backend 구현 | 상태 |
|---|---|---|---|
| Auth | 4 | 4 | Aligned |
| Members | 12 | 12 | Aligned |
| Pets | 8 | 8 | Aligned (`birthDate` 단일 입력 정렬 완료) |
| Threads | 9 | 9 | Aligned |
| Walk Diaries | 6 | 6 | Aligned |
| Chat | 14 | 14 | Aligned |
| Lost/Sighting | 7 | 7 | Aligned |
| Community | 10 | 10 | Aligned |
| Upload | 3 | 3 | Aligned |
| Settings (Local) | 0 (Local State) | N/A | Aligned (Local State) |

### 18.1 이번 라운드 반영 완료
- Walk/Chat/LostPet/Community 컨텍스트의 누락 Controller 엔드포인트 구현 완료
- Chat lifecycle 계약 정렬 완료 (`CHAT-LEAVE` POST, walk-confirm 3종, `CHAT-MSG-READ`, `CHAT-REVIEW-LIST`, `CHAT-REVIEW-ME` 스키마, WS CONNECT 인증)
- Community `STORY-LIST`, Upload presigned 2단계(`UPLOAD-PRESIGNED-POST/PUT`) 구현 반영 완료
- LostPet analyze 실패 정책 정렬 완료(`500 + L500_AI_ANALYZE_FAILED`, fallback 제거, 실패 시 세션 미생성)
- WalkDiary 에러코드 `WD404_THREAD_NOT_FOUND` 상태코드 404 정렬 완료
- 인덱스/유니크 보강 SQL을 `db/ddl/*.sql`로 분리하고 런타임 자동 적용 정책 고정
- Swagger(OpenAPI), Backend Entity/`db/ddl/*.sql` 동기화 완료

### 18.2 잔여 정렬 과제(활성 GAP)
- 없음 (Backend 범위 기준)
