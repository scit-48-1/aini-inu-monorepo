# 프론트엔드 개발 온보딩 가이드 (백엔드 실행 방식 정리)

## 0) 팀 운영 원칙

- 기본 권장 실행 방식은 **Docker**입니다.
- 필요 시 예외적으로 `./gradlew bootRun`으로도 실행할 수 있습니다.
- 실제 시크릿 값은 `.env.docker` 또는 `.env`로만 관리하며, 절대 커밋하지 않습니다.

## 실행 방식별 env 파일 규칙

- Docker 실행: `.env.docker.example`를 복사해 `.env.docker`를 사용
- 비Docker 실행(`./gradlew bootRun`): `.env.example`를 복사해 `.env`를 사용

## 1) 사전 준비

- Docker Desktop(또는 Docker Engine + Compose)
- Node.js / npm (프론트 프로젝트에서 이미 사용중일 것)
- Docker 엔진이 반드시 실행 중이어야 함 (Docker Desktop에서 Running 상태 확인)

실행 전 점검 명령:

```bash
docker info
docker compose version
```

`docker info`에서 에러가 나면 Docker 엔진이 꺼져 있는 상태이므로, Docker Desktop을 먼저 실행한 뒤 다시 진행합니다.

## 2) 시크릿 파일 전달 방식

`aini-inu-backend/.env.docker` 파일은 팀에서 별도로 전달받아야 합니다.
전달은 메신저 같은 안전한 경로를 사용합니다.

`.env.docker`는 절대 Git에 커밋하지 않습니다.

파일이 없다면 먼저 예시를 복사합니다.

```bash
cd aini-inu-backend
cp .env.docker.example .env.docker
```

그다음 전달받은 실제 값으로 `.env.docker`를 채웁니다.

## 3) `.env.docker` 환경변수

필수:
- `JWT_SECRET`
- `GEMINI_API_KEY`

선택(기능 사용 여부에 따라 설정):
- `GEMINI_EMBEDDING_MODEL`
- `LOSTPET_CHAT_BASE_URL`
- `LOSTPET_CHAT_DIRECT_CREATE_PATH`
- `LOSTPET_AI_VECTOR_TOP_K`
- `LOSTPET_SEARCH_SESSION_TTL_HOURS`
- `LOSTPET_SEARCH_TOP_N`
- `SPRING_AI_PGVECTOR_INITIALIZE_SCHEMA`
- `SPRING_AI_PGVECTOR_TABLE_NAME`
- `SPRING_AI_PGVECTOR_SCHEMA_NAME`
- `SPRING_AI_PGVECTOR_DIMENSIONS`
- `SPRING_AI_PGVECTOR_DISTANCE_TYPE`
- `SPRING_AI_PGVECTOR_INDEX_TYPE`
- `COMMUNITY_STORAGE_PRESIGNED_EXPIRES_SECONDS`

## 4) 백엔드 + DB 실행

아래 명령 한 번으로 PostgreSQL + 백엔드를 함께 실행합니다.

```bash
cd aini-inu-backend
./scripts/docker-up.sh
```

`Cannot connect to the Docker daemon` 오류가 나오면 Docker Desktop이 실행 중인지 먼저 확인합니다.

자주 쓰는 명령:

```bash
./scripts/docker-logs.sh           # 백엔드 로그 확인
./scripts/docker-logs.sh postgres  # DB 로그 확인
./scripts/docker-down.sh           # 전체 중지
```

## 5) 백엔드 정상 실행 확인

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### 5.1) OpenAPI 스냅샷 파일로 보기

서버를 매번 직접 확인하지 않도록 OpenAPI를 파일로 내보낼 수 있습니다.

```bash
cd aini-inu-backend
./scripts/export-openapi.sh
```

기본 출력 파일:
- `../common-docs/openapi/openapi.v1.json`

자주 쓰는 옵션:

```bash
./scripts/export-openapi.sh --port 18083
./scripts/export-openapi.sh --out ../common-docs/openapi/openapi.v1.json
./scripts/export-openapi.sh --wait-seconds 180
```

포트 충돌/기동 실패 시:
- `--port`로 빈 포트를 지정
- 부팅 로그 확인: `/tmp/aini_openapi_boot.log`

## 6) 프론트 로컬 연동 설정

프론트엔드 프로젝트 루트의 `.env.local` 파일을 생성하고 아래 값을 넣습니다.

```env
NEXT_PUBLIC_ENABLE_MSW=false
NEXT_PUBLIC_API_PROXY_TARGET=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

프론트 실행(프론트 프로젝트 루트에서):

```bash
npm run dev
```

## 7) Swagger에서 인증 API 테스트하기

1. `POST /api/v1/test/auth/token?memberId=1` 호출
2. 응답의 `data.accessToken` 복사
3. Swagger 우측 상단 `Authorize` 클릭
4. 토큰 값 입력(`Bearer` 접두어는 넣지 않음)
5. 인증이 필요한 API 테스트

## 8) 기본 시드 데이터 안내

백엔드 시작 시 아래 시드가 자동 적재됩니다.
- `db/seed/00_lookup_seed.sql`
- `db/seed/10_core_sample_seed.sql`
- `db/seed/20_status_edge_seed.sql`
- `db/seed/99_reset_sequences.sql`

예시 계정:
- `owner01@test.com` (`id=1`, `PET_OWNER`)
- `owner02@test.com` (`id=2`, `PET_OWNER`)
- `finder05@test.com` (`id=5`, `PET_OWNER`)
- `comm07@test.com` (`id=7`, `NON_PET_OWNER`)
- `inactive9001@test.com` (`id=9001`, `PET_OWNER`, `INACTIVE`)
- `banned9002@test.com` (`id=9002`, `NON_PET_OWNER`, `BANNED`, 로그인 제한)

## 9) 트러블슈팅

- `.env.docker` 파일이 없어서 `docker-up.sh`가 실패하는 경우:
  - `.env.docker.example` 복사 후 실제 값 입력
- 백엔드가 기동되지 않는 경우:
  - `./scripts/docker-logs.sh`로 오류 로그 확인
  - `8080`, `5432` 포트 충돌 여부 확인
- 프론트에서 API 호출이 실패하는 경우:
  - `NEXT_PUBLIC_API_PROXY_TARGET=http://localhost:8080` 확인
  - `docker compose ps`로 컨테이너 상태 확인
