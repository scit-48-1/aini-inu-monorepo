# GCP Docker 통합 배포 계획 & 실전 가이드

## 1. 목표

GCP 가상서버(VM) 하나에 DB, 백엔드, 프론트엔드, Nginx를 Docker Compose로 함께 띄워서
외부에서 HTTPS로 접속 가능하게 한다. iOS 모바일 앱 개발자도 동일한 백엔드를 사용한다.

---

## 2. 최종 구성도

```
브라우저 / iOS 앱 (https://ainiinu.kr)
    │
    ▼
┌──────────────────────────────────────┐
│ GCP VM (Debian 12 bookworm)          │
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
│                                      │
│  호스트에 설치:                       │
│  - certbot (SSL 인증서 발급/갱신)    │
│  - /etc/letsencrypt → nginx에 마운트 │
└──────────────────────────────────────┘
```

- 외부에는 Nginx의 80/443 포트만 노출
- 나머지 서비스는 Docker 내부 네트워크에서만 통신
- Nginx가 리버스 프록시 + SSL 터미네이션 역할 수행
- SSL 인증서는 호스트에서 certbot으로 직접 관리 (Docker certbot 컨테이너는 사용하지 않음)

---

## 3. 사전 준비

### 3.1 GCP Compute Engine 인스턴스 생성

| 항목 | 내용 |
|------|------|
| GCP VM | Compute Engine 인스턴스 생성 |
| OS | Debian 12 (bookworm) — GCP 기본 이미지 |
| 고정 IP | 외부 IP를 "고정 외부 IP"로 승격 (VM 재시작해도 IP 유지) |
| SSH 접속 | GCP 콘솔에서 "SSH" 버튼 클릭 (브라우저에서 바로 터미널 열림) |

### 3.2 도메인 DNS 설정

도메인 구매 후 GCP VM 외부 IP에 DNS A 레코드 연결.
DNS 제공자는 어디든 상관없음 (Route 53, Cloudflare, 가비아 등).

```
ainiinu.kr → 34.47.72.28 (GCP VM 외부 IP)
```

**확인 방법 (VM에서):**
```bash
sudo apt install -y dnsutils   # nslookup이 없으면 설치
nslookup ainiinu.kr            # IP가 VM 외부 IP와 일치하는지 확인
curl -s ifconfig.me            # VM의 외부 IP 확인
```

### 3.3 GCP 방화벽 설정

> **이것을 빠뜨리면 외부에서 접속이 안 되고, SSL 인증서 발급도 실패한다!**

GCP 콘솔 → VPC 네트워크 → 방화벽 → **방화벽 규칙 만들기:**

| 항목 | 값 |
|------|-----|
| 이름 | `allow-http-https` |
| 유형 | 인그레스 |
| 대상 | 전체 적용 |
| 소스 IP 범위 | `0.0.0.0/0` |
| 프로토콜/포트 | TCP: `80, 443` |
| 작업 | 허용 |
| 우선순위 | 1000 |

기본으로 존재하는 `default-allow-internal`은 GCP 내부 통신용이라 외부 접속에는 해당 없음.
`default-allow-ssh` (TCP 22)는 SSH 접속용이므로 건드리지 않는다.

### 3.4 비공개 Organization 레포지토리 clone

Organization 비공개 레포는 일반 `git clone`이 안 된다. **GitHub Personal Access Token (PAT)** 이 필요하다.

**토큰 생성 절차:**

1. GitHub → 우측 상단 프로필 → **Settings**
2. **Developer settings** → **Personal access tokens** → **Tokens (classic)**
3. **Generate new token (classic)** 클릭
4. **Select scopes** 에서 `repo` 체크
5. 생성 후 `ghp_xxxx...` 형태의 토큰 복사 (이 화면을 벗어나면 다시 볼 수 없음!)

**Organization 권한 부여 (중요!):**

Personal 토큰을 만들어도 Organization 레포에는 바로 접근이 안 된다.
토큰 생성 후 추가 승인이 필요하다:

1. GitHub → Settings → Developer settings → Personal access tokens
2. 생성한 토큰 클릭
3. 하단 **Organization access** 섹션에서 해당 org 옆 **"Grant"** 또는 **"Authorize"** 클릭

만약 Grant 버튼이 안 보이면, Organization 설정에서 PAT 접근을 허용해야 한다:
- GitHub → Organization 페이지 → **Settings** → **Third-party access** → **Personal access tokens** → 정책을 **"Allow"**로 변경

**clone 명령:**
```bash
git clone https://ghp_xxxx@github.com/<org>/<repo>.git
```

---

## 4. Docker & Docker Compose 설치

### 4.1 GCP VM의 OS 확인

> **GCP Compute Engine의 기본 이미지는 Ubuntu가 아니라 Debian이다!**
> 이것을 모르면 Docker 설치에서 삽질하게 된다.

```bash
lsb_release -cs
# → bookworm (Debian 12)
```

### 4.2 Docker 설치 시 주의사항

**흔한 실수와 에러:**

| 시도한 방법 | 결과 | 원인 |
|------------|------|------|
| `sudo apt install docker.io` | Docker는 설치되지만 `docker compose` 명령 없음 | `docker.io`는 Debian/Ubuntu 기본 패키지로, compose 플러그인 미포함 |
| `sudo apt install docker-compose-plugin` | `Unable to locate package` | Docker 공식 저장소가 등록되지 않은 상태 |
| Ubuntu용 Docker 저장소 추가 후 설치 | `Package 'docker-ce' has no installation candidate` | GCP VM이 Ubuntu가 아니라 Debian인데 Ubuntu 저장소를 추가함 |

**Docker Desktop은 설치 불가** — GCP VM은 GUI가 없는 서버이므로 Docker Desktop(Mac/Windows 데스크탑용)은 사용할 수 없다.

### 4.3 올바른 설치 방법 (Debian 12 bookworm)

```bash
# 1. 필수 도구 설치
sudo apt update
sudo apt install -y ca-certificates curl gnupg

# 2. Docker 공식 GPG 키 추가
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 3. Debian용 Docker 저장소 추가 (Ubuntu가 아님에 주의!)
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 4. Docker + Compose 플러그인 설치
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 5. 현재 유저에게 docker 실행 권한 부여
sudo usermod -aG docker $USER

# 6. SSH 재접속 (권한 적용)
exit
# GCP 콘솔에서 다시 SSH 접속

# 7. 설치 확인
docker --version
docker compose version
```

### 4.4 AWS EC2 기준 (참고)

**Ubuntu 기준:**
```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo usermod -aG docker $USER
# SSH 재접속 후 적용
```

**Amazon Linux 기준:**
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

## 5. 프로젝트에서 생성한 파일들

### 5.1 프론트엔드 Dockerfile

`aini-inu-frontend/Dockerfile`:

```dockerfile
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

ENV NEXT_PUBLIC_ENABLE_MSW=false
ENV NEXT_PUBLIC_API_PROXY_TARGET=http://backend:8080
ENV NEXT_PUBLIC_WS_URL=ws://backend:8080

RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
```

- 3단계 멀티스테이지 빌드: deps(의존성) → builder(빌드) → runner(실행)
- `next.config.ts`에 `output: 'standalone'` 추가 필요
- standalone 빌드 시 `.next/static`과 `public`은 자동 포함되지 않으므로 반드시 별도 복사
- 보안을 위해 비루트(nextjs) 유저로 실행

### 5.2 Nginx 설정 파일

`nginx/nginx.conf`:

```nginx
# Rate Limiting: IP당 초당 요청 수 제한
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=ws:10m rate=5r/s;

# --- HTTP → HTTPS 리다이렉트 ---
server {
    listen 80;
    server_name ${DOMAIN};

    # Let's Encrypt 인증서 발급/갱신용
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# --- HTTPS 메인 서버 ---
server {
    listen 443 ssl;
    server_name ${DOMAIN};

    # SSL 인증서 (호스트의 /etc/letsencrypt를 마운트)
    ssl_certificate     /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;

    # SSL 보안 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 공통 프록시 헤더
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # REST API → backend
    location /api/v1/ {
        limit_req zone=api burst=20 nodelay;
        proxy_pass http://backend:8080;
        client_max_body_size 10m;   # 이미지 업로드용
    }

    # WebSocket → backend
    location /ws/ {
        limit_req zone=ws burst=10 nodelay;
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;   # WebSocket 연결 유지
        proxy_send_timeout 3600s;
    }

    # 그 외 전부 → frontend
    location / {
        proxy_pass http://frontend:3000;
    }
}
```

- Nginx의 `templates/` 방식으로 마운트: `${DOMAIN}` 환경변수가 컨테이너 시작 시 자동 치환됨
- Rate Limiting: API는 IP당 10req/s, WebSocket은 5req/s
- WebSocket 프록시: `Upgrade` + `Connection: upgrade` 헤더 필수
- 이미지 업로드: `client_max_body_size 10m` 설정

### 5.3 루트 docker-compose.yml

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: aini-inu-postgres
    restart: unless-stopped
    environment:
      TZ: Asia/Seoul
      POSTGRES_DB: ${POSTGRES_DB:-ainiinu}
      POSTGRES_USER: ${POSTGRES_USER:-ainiinu}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-ainiinu}
    volumes:
      - aini_inu_postgres_data:/var/lib/postgresql/data
      - ./aini-inu-backend/docker/postgres/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-ainiinu} -d ${POSTGRES_DB:-ainiinu}"]
      interval: 5s
      timeout: 5s
      retries: 30

  backend:
    build:
      context: ./aini-inu-backend
      dockerfile: Dockerfile
    container_name: aini-inu-backend
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    env_file:
      - .env.docker
    environment:
      TZ: Asia/Seoul
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-ainiinu}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-ainiinu}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-ainiinu}
      COMMUNITY_STORAGE_LOCAL_BASE_DIR: /app/var/uploads
      COMMUNITY_STORAGE_PUBLIC_BASE_URL: https://${DOMAIN}
    volumes:
      - aini_inu_backend_uploads:/app/var/uploads

  frontend:
    build:
      context: ./aini-inu-frontend
      dockerfile: Dockerfile
    container_name: aini-inu-frontend
    restart: unless-stopped
    depends_on:
      - backend

  nginx:
    image: nginx:alpine
    container_name: aini-inu-nginx
    restart: unless-stopped
    depends_on:
      - frontend
      - backend
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/templates/default.conf.template:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - certbot_webroot:/var/www/certbot:ro
    environment:
      DOMAIN: ${DOMAIN}

volumes:
  aini_inu_postgres_data:
  aini_inu_backend_uploads:
  certbot_webroot:
```

핵심 포인트:
- 외부 포트는 nginx의 80/443만 노출 (postgres, backend, frontend는 Docker 내부 전용)
- SSL 인증서는 호스트의 `/etc/letsencrypt`를 직접 마운트 (Docker 볼륨 아님)
- `env_file: .env.docker`로 백엔드 환경변수 주입
- docker compose는 기본적으로 `.env` 파일을 읽으므로 `.env.docker`를 `.env`로도 복사해야 함

### 5.4 환경변수 파일

`.env.docker.example` (커밋 대상 — 템플릿):

```env
# 도메인
DOMAIN=your-domain.com

# DB (Docker 내부 통신용)
POSTGRES_DB=ainiinu
POSTGRES_USER=ainiinu
POSTGRES_PASSWORD=change-me-in-production

# JWT
JWT_SECRET=your-super-secret-key-must-be-at-least-32-characters-long-for-hs256-algorithm

# Gemini AI
GEMINI_API_KEY=
GEMINI_EMBEDDING_MODEL=gemini-embedding-001

# Lost Pet AI (optional)
LOSTPET_AI_VECTOR_TOP_K=50
LOSTPET_SEARCH_SESSION_TTL_HOURS=24
LOSTPET_SEARCH_TOP_N=20

# PgVector (optional)
SPRING_AI_PGVECTOR_INITIALIZE_SCHEMA=false
SPRING_AI_PGVECTOR_TABLE_NAME=lostpet_vector_store
SPRING_AI_PGVECTOR_SCHEMA_NAME=public
SPRING_AI_PGVECTOR_DIMENSIONS=768
SPRING_AI_PGVECTOR_DISTANCE_TYPE=COSINE_DISTANCE
SPRING_AI_PGVECTOR_INDEX_TYPE=HNSW

# Community Storage (optional)
COMMUNITY_STORAGE_PRESIGNED_EXPIRES_SECONDS=300
```

**중요:** `.env.docker`와 `.env`는 `.gitignore`에 추가하여 절대 커밋하지 않는다.

---

## 6. VM에서 배포하기 (실전 순서)

### 6.1 레포지토리 clone

```bash
git clone https://ghp_xxxx@github.com/<org>/aini-inu-monorepo.git
cd aini-inu-monorepo
```

### 6.2 환경변수 설정

```bash
cp .env.docker.example .env.docker
nano .env.docker    # 실제 값으로 편집 (DOMAIN, JWT_SECRET, GEMINI_API_KEY 등)
cp .env.docker .env # docker compose가 .env를 기본으로 읽으므로 복사 필요
```

> **`.env` 파일을 만들지 않으면** `WARN: The "DOMAIN" variable is not set` 경고가 발생하고
> nginx의 `${DOMAIN}` 치환이 빈 문자열이 되어 SSL이 작동하지 않는다.

### 6.3 SSL 인증서 발급 (최초 1회)

> **반드시 docker compose up 전에 실행해야 한다!** (80 포트가 비어있어야 함)

```bash
# 1. certbot 설치
sudo apt install -y certbot

# 2. 모든 컨테이너 중지 (80 포트 확보)
docker compose down

# 3. 인증서 발급
sudo certbot certonly --standalone -d ainiinu.kr --agree-tos -m your@email.com
```

성공하면:
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/ainiinu.kr/fullchain.pem
Key is saved at:          /etc/letsencrypt/live/ainiinu.kr/privkey.pem
```

**SSL 인증서 발급이 안 되는 경우 체크리스트:**

| 증상 | 원인 | 해결 |
|------|------|------|
| certbot 명령어 자체가 Creating 에서 멈춤 | Docker certbot 컨테이너 방식의 문제 | 호스트에 직접 `sudo apt install certbot` 후 standalone 모드 사용 |
| certbot이 무한 대기 | 80 포트를 nginx가 점유 중 | `docker compose down`으로 모든 컨테이너 중지 후 재시도 |
| Let's Encrypt 검증 실패 | GCP 방화벽에서 80 포트 미허용 | VPC 방화벽에 TCP 80, 443 인바운드 규칙 추가 |
| DNS 검증 실패 | 도메인이 VM IP를 가리키지 않음 | `nslookup ainiinu.kr` 결과가 VM 외부 IP와 일치하는지 확인 |

### 6.4 전체 서비스 기동

```bash
docker compose up -d --build
```

첫 빌드는 시간이 걸린다 (백엔드 Gradle 빌드 + 프론트엔드 npm build).

### 6.5 상태 확인

```bash
# 서비스 상태 확인 — 4개 모두 Up이면 성공
docker compose ps

# 문제 시 로그 확인
docker compose logs -f --tail=50

# 특정 서비스 로그만 보기
docker compose logs -f backend
docker compose logs -f nginx
docker compose logs -f frontend
```

### 6.6 접속 확인

```
https://ainiinu.kr          → 프론트엔드 (웹 페이지)
https://ainiinu.kr/api/v1/  → 백엔드 API
wss://ainiinu.kr/ws/        → WebSocket
```

포트번호 없이 도메인만으로 접속 가능 (nginx가 80/443 표준 포트 사용).

### 6.7 인증서 자동 갱신

certbot을 `sudo apt install`로 설치하면 자동 갱신 타이머가 함께 설정된다.
수동 갱신이 필요한 경우:

```bash
# nginx 중지 → 갱신 → nginx 재시작
docker compose stop nginx
sudo certbot renew
docker compose start nginx
```

---

## 7. 소스코드 점검 결과

### 7.1 CORS — 문제 없음

- `WebConfig.java`: `setAllowedOriginPatterns("*")` + `allowCredentials(true)`
- `SecurityConfig.java`: Security 필터 레벨에서도 동일한 CORS 설정 적용
- `WebSocketConfig.java`: WebSocket도 `setAllowedOriginPatterns("*")`
- Docker/GCP 배포 시 도메인이 바뀌어도 코드 수정 불필요

### 7.2 WebSocket (채팅) — 문제 없음

- STOMP 엔드포인트: `/ws/chat-rooms/{roomId}`
- 인증: STOMP CONNECT 시 `Authorization: Bearer <JWT>` 헤더로 처리
- 메시지 브로커: 인메모리 (`/topic`, `/queue`)
- 이벤트 발행: `/topic/chat-rooms/{roomId}/events`
- 프론트엔드: `NEXT_PUBLIC_WS_URL` 환경변수로 WebSocket URL 결정
- 프로토콜 자동 감지: HTTPS → `wss://`, HTTP → `ws://`
- 실패 시 HTTP 폴링 fallback 구현되어 있음
- Nginx에서 `/ws/*` 프록시 시 upgrade 헤더만 설정하면 동작

### 7.3 이미지 업로드/조회 — 문제 없음 (주의사항 있음)

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
- iOS: base URL(`https://ainiinu.kr`)을 앞에 붙여서 조합 필요

---

## 8. iOS 모바일 앱 개발자 연동 가이드

### 접근 방식

```
iOS 앱 → https://ainiinu.kr/api/v1/... → Nginx → backend
iOS 앱 → wss://ainiinu.kr/ws/...       → Nginx → backend
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
| WebSocket | `wss://ainiinu.kr/ws/chat-rooms/{roomId}` + STOMP + JWT 헤더 |
| 푸시 알림 | 추후 필요 시 백엔드에 APNs 연동 추가 |

---

## 9. 트러블슈팅 모음

### 9.1 Docker 설치 관련

**Q: `sudo apt install docker.io` 했는데 `docker compose` 명령이 없다?**

`docker.io`는 Debian/Ubuntu 기본 패키지로 Docker Compose 플러그인이 포함되지 않는다.
Docker 공식 저장소에서 `docker-ce` + `docker-compose-plugin`을 설치해야 한다. (4장 참고)

**Q: `Unable to locate package docker-compose-plugin`?**

Docker 공식 저장소가 시스템에 등록되지 않은 상태. 4.3절의 GPG 키 + 저장소 추가 과정을 먼저 수행해야 한다.

**Q: `Package 'docker-ce' has no installation candidate`?**

Ubuntu용 저장소를 추가했는데 실제 OS가 Debian인 경우. `lsb_release -cs`로 OS를 확인하고, 결과가 `bookworm`이면 Debian 12이므로 저장소 URL을 `https://download.docker.com/linux/debian`으로 변경해야 한다.

**Q: Docker Desktop을 설치하면 안 되나?**

GCP VM은 GUI가 없는 서버이므로 Docker Desktop은 설치할 수 없다. Docker Desktop은 Mac/Windows 데스크탑 환경 전용이다.

### 9.2 비공개 레포 clone 관련

**Q: Organization 비공개 레포를 clone하면 권한 에러가 난다?**

GitHub PAT (Personal Access Token)을 생성하고, Organization access에서 해당 org에 **"Grant"** 버튼을 클릭해야 한다. 개인 토큰을 만들었다고 자동으로 org 레포에 접근할 수 있는 것이 아니다.

### 9.3 환경변수 관련

**Q: `WARN: The "DOMAIN" variable is not set. Defaulting to a blank string.`?**

docker compose는 기본적으로 프로젝트 루트의 `.env` 파일을 읽는다. `.env.docker`만 만들고 `.env`를 만들지 않으면 이 경고가 발생한다.

```bash
cp .env.docker .env
```

### 9.4 SSL 인증서 관련

**Q: `docker compose run --rm certbot ...` 이 Creating에서 멈춘다?**

Docker certbot 컨테이너 방식은 여러 이유로 멈출 수 있다. 호스트에 직접 certbot을 설치하는 것이 가장 확실하다:

```bash
docker compose down              # 80 포트 해제
sudo apt install -y certbot      # 호스트에 직접 설치
sudo certbot certonly --standalone -d ainiinu.kr --agree-tos -m your@email.com
```

**Q: certbot standalone도 멈춘다?**

GCP 방화벽에서 TCP 80 포트가 열려있는지 확인. Let's Encrypt 서버가 `http://도메인:80`으로 검증 요청을 보내는데, 방화벽에서 차단되면 무한 대기한다.

---

## 10. HTTPS 방식 비교 (참고)

| 방법 | 장점 | 단점 |
|------|------|------|
| Nginx + Let's Encrypt (채택) | compose로 함께 관리, 자동갱신 가능 | Nginx 설정 필요 |
| Caddy | 설정 2~3줄, 인증서 자동 발급/갱신 | Nginx보다 덜 익숙할 수 있음 |
| GCP Load Balancer | 인프라 레벨 처리, 컨테이너 수정 없음 | GCP 비용 추가, 설정 복잡 |
| Cloudflare Proxy | DNS만 변경하면 끝, 무료 SSL | 외부 서비스 의존 |

Nginx를 Docker Compose 안에 포함하는 방식을 채택한 이유:
- VM에 Docker + certbot 외 설치할 것이 없음
- `docker compose up -d` 한 번으로 전체 기동
- 서버 이전 시 compose 파일만 가져가면 끝
- 컨테이너 간 통신이 Docker 내부 네트워크로 깔끔

---

## 11. 운영 명령어 치트시트

```bash
# 전체 서비스 시작 (빌드 포함)
docker compose up -d --build

# 전체 서비스 중지
docker compose down

# 서비스 상태 확인
docker compose ps

# 전체 로그 (최근 50줄부터 실시간)
docker compose logs -f --tail=50

# 특정 서비스 로그
docker compose logs -f backend
docker compose logs -f nginx
docker compose logs -f frontend
docker compose logs -f postgres

# 특정 서비스만 재시작
docker compose restart backend
docker compose restart nginx

# 코드 업데이트 후 재배포
git pull
docker compose up -d --build

# SSL 인증서 수동 갱신
docker compose stop nginx
sudo certbot renew
docker compose start nginx

# DB 데이터는 유지하면서 컨테이너만 재생성
docker compose down
docker compose up -d --build

# 모든 것을 초기화 (DB 데이터 포함 삭제 — 주의!)
docker compose down -v
```

---

## 12. 결론

- 현재 소스코드에서 **코드 수정 최소** (next.config.ts에 `output: 'standalone'` 한 줄)로 Docker 배포 가능
- 프로젝트에 추가한 파일: 프론트엔드 Dockerfile, .dockerignore, nginx.conf, 루트 docker-compose.yml, .env.docker.example
- GCP VM(Debian 12)에 Docker 공식 저장소에서 설치, SSL은 호스트 certbot으로 발급
- `docker compose up -d --build`로 전체 서비스 기동
- `https://ainiinu.kr`로 웹과 iOS 모두 동일한 도메인으로 접속
