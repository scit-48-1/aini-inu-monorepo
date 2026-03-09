# GCP Docker 통합 배포 계획

## 1. 목표

GCP 가상서버(VM) 하나에 DB, 백엔드, 프론트엔드, Nginx를 Docker Compose로 함께 띄워서
외부에서 HTTPS로 접속 가능하게 한다. iOS 모바일 앱 개발자도 동일한 백엔드를 사용한다.

---

## 2. 최종 구성도

```
브라우저 / iOS 앱 (https://your-domain.com)
    │
    ▼
┌──────────────────────────────────────┐
│ GCP VM                               │
│                                      │
│  Docker Compose                      │
│  ┌────────────────────────────────┐  │
│  │ nginx (:80/:443 외부 노출)     │  │
│  │  ├─ /            → frontend   │  │
│  │  ├─ /api/v1/*    → backend    │  │
│  │  └─ /ws/*        → backend    │  │
│  ├────────────────────────────────┤  │
│  │ frontend (:3000 내부만)        │  │
│  ├────────────────────────────────┤  │
│  │ backend  (:8080 내부만)        │  │
│  ├────────────────────────────────┤  │
│  │ postgres (:5432 내부만)        │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
```

- 외부에는 Nginx의 80/443 포트만 노출
- 나머지 서비스는 Docker 내부 네트워크에서만 통신
- Nginx가 리버스 프록시 + SSL 터미네이션 역할 수행

---

## 3. 사전 준비

### GCP 기준

| 항목 | 내용 |
|------|------|
| GCP VM | Compute Engine 인스턴스 생성, Docker/Docker Compose 설치 |
| 도메인 | 구매 후 GCP VM 외부 IP에 DNS A레코드 연결 |
| GCP 방화벽 | VPC 방화벽 규칙에서 80, 443 포트 인바운드 허용 |
| SSH 접속 | GCP 콘솔에서 "SSH" 버튼 클릭 (브라우저에서 바로 터미널 열림) |
| 고정 IP | 외부 IP를 "고정 외부 IP"로 승격 (VM 재시작해도 IP 유지) |

### AWS EC2 기준

| 항목 | 내용 |
|------|------|
| EC2 인스턴스 | EC2 콘솔에서 인스턴스 생성 (Ubuntu 추천, t3.small 이상) |
| 키 페어 | 인스턴스 생성 시 `.pem` 키 페어 다운로드 (SSH 접속에 필요) |
| 도메인 | 구매 후 Elastic IP에 DNS A레코드 연결 |
| Security Group | 인바운드 규칙에서 80, 443 포트 허용 (SSH용 22는 기본 허용) |
| SSH 접속 | `ssh -i your-key.pem ubuntu@<Elastic-IP>` 또는 EC2 콘솔 "Connect" 버튼 |
| Elastic IP | EC2 콘솔 → Elastic IP 할당 → 인스턴스에 연결 (VM 재시작해도 IP 유지) |

**AWS Docker 설치 (Ubuntu 기준):**
```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo usermod -aG docker $USER
# SSH 재접속 후 적용
```

**AWS Docker 설치 (Amazon Linux 기준):**
```bash
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -aG docker $USER
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
# SSH 재접속 후 적용
```

> Docker 설치 이후의 모든 과정 (git clone, docker compose up, Nginx, SSL 등)은 GCP/AWS 동일하다.

---

## 4. 프로젝트에서 할 작업 (파일 4~5개)

### 4.1 프론트엔드 Dockerfile 생성

`aini-inu-frontend/Dockerfile` 생성:
- Node.js 기반 멀티스테이지 빌드
- Next.js `standalone` 출력 모드 사용 (경량 프로덕션 이미지)
- `next.config.ts`에 `output: 'standalone'` 추가 필요
- 빌드 시 `NEXT_PUBLIC_API_PROXY_TARGET=http://backend:8080` 설정
- MSW 비활성화 (`NEXT_PUBLIC_ENABLE_MSW=false`)
- standalone 빌드 시 아래 3가지를 반드시 복사 (Next.js 정석):
  ```dockerfile
  COPY --from=builder /app/.next/standalone ./      # 서버 코드 (server.js + node_modules)
  COPY --from=builder /app/.next/static ./.next/static  # JS/CSS 번들 (standalone에 자동 미포함)
  COPY --from=builder /app/public ./public           # 정적 파일 (로고, favicon, dog-portraits 등)
  ```

### 4.2 Nginx 설정 파일 생성

`nginx/nginx.conf` (프로젝트 루트 기준):

```
라우팅 규칙:
  /            → frontend:3000   (웹 페이지)
  /api/v1/*    → backend:8080    (REST API)
  /ws/*        → backend:8080    (WebSocket, upgrade 헤더 포함)
```

WebSocket 프록시 시 반드시 포함할 헤더:
```
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

SSL 설정:
- Let's Encrypt 인증서 경로 마운트
- HTTP → HTTPS 리다이렉트
- SSL 프로토콜/암호화 설정

Rate Limiting (비정상 요청 차단):
```nginx
# IP당 초당 요청 수 제한
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;

server {
    location /api/v1/ {
        limit_req zone=api burst=20 nodelay;
        # rate=10r/s  — IP당 초당 10개 요청 허용
        # burst=20    — 순간적으로 20개까지 허용, 초과 시 429 반환
        # nodelay     — burst 내 요청은 지연 없이 즉시 처리
        proxy_pass http://backend:8080;
    }
}
```
- `$binary_remote_addr` 기준으로 IP별 요청 수 추적
- 임계치 초과 시 자동으로 429(Too Many Requests) 반환
- API, WebSocket, 정적 페이지별로 별도 zone 설정 가능

### 4.3 루트 docker-compose.yml 생성

기존 `aini-inu-backend/docker-compose.yml`을 기반으로 확장:

```yaml
services:
  postgres:     # pgvector:pg16, 내부만
  backend:      # Spring Boot, 내부만
  frontend:     # Next.js standalone, 내부만
  nginx:        # 80/443 외부 노출

volumes:
  aini_inu_postgres_data:    # DB 데이터 영속화
  aini_inu_backend_uploads:  # 업로드 이미지 영속화
  certbot_certs:             # SSL 인증서
```

서비스 간 의존관계:
```
postgres → backend → frontend → nginx
          (healthcheck)
```

### 4.4 루트 .env.docker 생성

```env
# 도메인
DOMAIN=your-domain.com

# JWT
JWT_SECRET=<시크릿>

# Gemini AI
GEMINI_API_KEY=<API키>
GEMINI_EMBEDDING_MODEL=text-embedding-004

# DB (내부 통신용)
POSTGRES_DB=ainiinu
POSTGRES_USER=ainiinu
POSTGRES_PASSWORD=<비밀번호>

# 기타 백엔드 설정
COMMUNITY_STORAGE_LOCAL_BASE_DIR=/app/var/uploads
COMMUNITY_STORAGE_PUBLIC_BASE_URL=https://your-domain.com
```

- `.gitignore`에 추가하여 커밋 방지
- `.env.docker.example` 템플릿은 커밋

---

## 5. VM에서 할 작업 (GCP/AWS 공통)

### 5.1 초기 SSL 인증서 발급

```bash
# certbot으로 Let's Encrypt 인증서 발급
# 또는 compose에 certbot 서비스 포함하여 자동화
```

### 5.2 배포

```bash
git clone <repo>
cp .env.docker.example .env.docker   # 환경변수 편집
docker compose up -d                  # 전체 서비스 기동
```

### 5.3 인증서 자동 갱신

- certbot 컨테이너를 compose에 포함하거나
- VM cron으로 주기적 갱신 설정

---

## 6. 소스코드 점검 결과

### 6.1 CORS — 문제 없음

- `WebConfig.java`: `setAllowedOriginPatterns("*")` + `allowCredentials(true)`
- `SecurityConfig.java`: Security 필터 레벨에서도 동일한 CORS 설정 적용
- `WebSocketConfig.java`: WebSocket도 `setAllowedOriginPatterns("*")`
- Docker/GCP 배포 시 도메인이 바뀌어도 코드 수정 불필요

### 6.2 WebSocket (채팅) — 문제 없음

- STOMP 엔드포인트: `/ws/chat-rooms/{roomId}`
- 인증: STOMP CONNECT 시 `Authorization: Bearer <JWT>` 헤더로 처리
- 메시지 브로커: 인메모리 (`/topic`, `/queue`)
- 이벤트 발행: `/topic/chat-rooms/{roomId}/events`
- 프론트엔드: `NEXT_PUBLIC_WS_URL` 환경변수로 WebSocket URL 결정
- 프로토콜 자동 감지: HTTPS → `wss://`, HTTP → `ws://`
- 실패 시 HTTP 폴링 fallback 구현되어 있음
- Nginx에서 `/ws/*` 프록시 시 upgrade 헤더만 설정하면 동작

### 6.3 이미지 업로드/조회 — 문제 없음 (주의사항 있음)

**동작 방식:**
1. 프론트엔드 → `POST /api/v1/images/presigned-url` (presigned URL 요청)
2. 프론트엔드 → `PUT /api/v1/images/presigned-upload/{token}` (파일 업로드)
3. DB에 상대경로 저장: `/api/v1/images/local?key=community/post/...`
4. 이미지 조회: `GET /api/v1/images/local?key=...` (인증 불필요, `@Public`)

**Docker 환경:**
- `application.properties` 기본값: `../common-docs/storage` (로컬 개발용 상대경로)
- Docker compose에서 `COMMUNITY_STORAGE_LOCAL_BASE_DIR=/app/var/uploads`로 오버라이드
- Docker volume으로 영속화 — 올바르게 처리되어 있음

**주의: 이미지 URL이 상대경로**
- 백엔드가 `/api/v1/images/local?key=...` 형태의 상대경로를 반환
- 웹: Next.js rewrites → Nginx를 통해 정상 동작
- iOS: base URL(`https://your-domain.com`)을 앞에 붙여서 조합 필요

---

## 7. iOS 모바일 앱 개발자 연동 가이드

### 접근 방식

```
iOS 앱 → https://your-domain.com/api/v1/... → Nginx → backend
iOS 앱 → wss://your-domain.com/ws/...       → Nginx → backend
```

Nginx가 `/api/v1/*`과 `/ws/*`를 백엔드로 라우팅하므로,
iOS 앱은 웹 프론트엔드와 동일한 도메인의 동일한 경로로 API를 호출하면 된다.

### 고려사항

| 항목 | 내용 |
|------|------|
| HTTPS 필수 | iOS ATS 정책상 HTTP 차단 (HTTPS 배포이므로 문제 없음) |
| CORS 불필요 | 네이티브 앱은 CORS 제약 없음 |
| API 문서 | `common-docs/openapi/openapi.v1.json` 공유 |
| 인증 | JWT 토큰 발급/갱신 흐름 동일하게 구현 |
| 이미지 URL | 백엔드 응답의 상대경로에 base URL 조합 필요 |
| WebSocket | `wss://your-domain.com/ws/chat-rooms/{roomId}` + STOMP + JWT 헤더 |
| 푸시 알림 | 추후 필요 시 백엔드에 APNs 연동 추가 |

---

## 8. HTTPS 방식 비교 (참고)

| 방법 | 장점 | 단점 |
|------|------|------|
| Nginx + Let's Encrypt (채택) | compose로 함께 관리, 자동갱신 가능 | Nginx 설정 필요 |
| Caddy | 설정 2~3줄, 인증서 자동 발급/갱신 | Nginx보다 덜 익숙할 수 있음 |
| GCP Load Balancer | 인프라 레벨 처리, 컨테이너 수정 없음 | GCP 비용 추가, 설정 복잡 |
| Cloudflare Proxy | DNS만 변경하면 끝, 무료 SSL | 외부 서비스 의존 |

Nginx를 Docker Compose 안에 포함하는 방식을 채택한 이유:
- VM에 Docker 외 설치할 것이 없음
- `docker compose up -d` 한 번으로 전체 기동
- 서버 이전 시 compose 파일만 가져가면 끝
- 컨테이너 간 통신이 Docker 내부 네트워크로 깔끔

---

## 9. 결론

- 현재 소스코드에서 **코드 수정 없이** Docker 배포 가능
- 필요한 작업: 프론트엔드 Dockerfile, Nginx 설정, 루트 docker-compose.yml, .env.docker 생성
- `next.config.ts`에 `output: 'standalone'` 한 줄 추가
- GCP VM에서 `docker compose up -d`로 전체 서비스 기동
- 웹과 iOS 모두 동일한 도메인으로 접속
