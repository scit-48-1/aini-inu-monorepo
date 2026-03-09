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
| 디스크 | **최소 20GB 이상 권장** (기본 10GB는 Docker 빌드 시 용량 부족) |
| 고정 IP | 외부 IP를 "고정 외부 IP"로 승격 (VM 재시작해도 IP 유지) |
| SSH 접속 | GCP 콘솔에서 "SSH" 버튼 클릭 (브라우저에서 바로 터미널 열림) |

> **디스크 크기를 반드시 20GB 이상으로 설정하자!**
>
> GCP 기본 디스크는 10GB인데, Docker 이미지 빌드 시 다음 용량이 필요하다:
> - Docker 엔진 + 이미지 레이어 캐시: ~2GB
> - 백엔드 빌드 (Gradle + JDK + 의존성): ~3GB
> - 프론트엔드 빌드 (Node.js + node_modules): ~2GB
> - PostgreSQL 데이터: ~500MB~
> - OS + 시스템: ~2GB
>
> 10GB로는 빌드 도중 `no space left on device` 에러가 발생한다.
> **인스턴스 생성 시 20~30GB로 설정하면 안전하다.**
>
> 이미 만든 인스턴스의 디스크를 늘리는 방법은 10.6절 참고.

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
4. **Select scopes** 에서 `repo` 체크 — 이것은 "이 토큰이 비공개 레포에 접근할 수 있는 권한"을 의미
5. 생성 후 `ghp_xxxx...` 형태의 토큰 복사 (이 화면을 벗어나면 다시 볼 수 없음!)

> **`repo` 스코프란?** GitHub 토큰은 할 수 있는 일의 범위(scope)를 지정한다.
> `repo`를 체크하면 비공개 레포 읽기/쓰기 권한이 부여된다.
> clone만 할 거면 이것 하나만 체크하면 충분하다.

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

> **`lsb_release` 란?** Linux Standard Base release. OS 배포판 정보를 확인하는 명령어.
> `-c`는 코드명(codename), `-s`는 짧은 출력(short). `bookworm`이 나오면 Debian 12.

### 4.2 Docker 설치 시 주의사항

**흔한 실수와 에러:**

| 시도한 방법 | 결과 | 원인 |
|------------|------|------|
| `sudo apt install docker.io` | Docker는 설치되지만 `docker compose` 명령 없음 | `docker.io`는 Debian/Ubuntu 기본 패키지로, compose 플러그인 미포함 |
| `sudo apt install docker-compose-plugin` | `Unable to locate package` | Docker 공식 저장소가 등록되지 않은 상태 |
| Ubuntu용 Docker 저장소 추가 후 설치 | `Package 'docker-ce' has no installation candidate` | GCP VM이 Ubuntu가 아니라 Debian인데 Ubuntu 저장소를 추가함 |

**Docker Desktop은 설치 불가** — GCP VM은 GUI가 없는 서버이므로 Docker Desktop(Mac/Windows 데스크탑용)은 사용할 수 없다.

> **`docker.io` vs `docker-ce` 차이:**
> - `docker.io` — Debian/Ubuntu 공식 패키지 저장소에서 제공하는 Docker. 버전이 느리고 compose 플러그인 미포함.
> - `docker-ce` — Docker 공식 저장소에서 제공하는 Community Edition. 최신 버전 + compose 플러그인 포함.
> - 결론: **항상 `docker-ce`를 사용하자.**

### 4.3 올바른 설치 방법 (Debian 12 bookworm)

```bash
# 1. 필수 도구 설치
sudo apt update
sudo apt install -y ca-certificates curl gnupg

# 2. Docker 공식 GPG 키 추가
#    GPG 키 = 패키지가 진짜 Docker에서 만든 것인지 검증하는 서명 키
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# 3. Debian용 Docker 저장소 추가 (Ubuntu가 아님에 주의!)
#    이 줄이 apt에게 "Docker 패키지는 이 URL에서 다운받아라"고 알려줌
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian bookworm stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 4. Docker + Compose 플러그인 설치
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 5. 현재 유저에게 docker 실행 권한 부여
#    이걸 안 하면 매번 sudo docker ... 로 실행해야 함
sudo usermod -aG docker $USER

# 6. SSH 재접속 (권한 적용 — 재접속해야 그룹 변경이 반영됨)
exit
# GCP 콘솔에서 다시 SSH 접속

# 7. 설치 확인
docker --version          # Docker 버전 출력
docker compose version    # Docker Compose 버전 출력
```

> **각 패키지 역할:**
> - `docker-ce` — Docker 엔진 (컨테이너 실행의 핵심)
> - `docker-ce-cli` — `docker` 명령어 (CLI 도구)
> - `containerd.io` — 컨테이너 런타임 (Docker 내부에서 실제로 컨테이너를 관리)
> - `docker-compose-plugin` — `docker compose` 명령어 (여러 컨테이너를 한번에 관리)

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

> **멀티스테이지 빌드란?**
> 하나의 Dockerfile 안에서 여러 단계(`FROM`)를 거치는 빌드 방식.
> 빌드에 필요한 도구(node_modules 전체, 빌드 도구 등)는 builder 단계에서만 쓰고,
> 최종 이미지(runner)에는 실행에 필요한 최소한만 복사한다.
> 결과: 이미지 크기가 수 GB → 수백 MB로 줄어듦.

> **`npm ci` vs `npm install` 차이:**
> - `npm install` — `package.json` 기준으로 의존성 해석, `package-lock.json` 수정 가능
> - `npm ci` — `package-lock.json`을 **정확히** 따름. 더 빠르고 재현 가능. CI/Docker 환경에 적합.

> **standalone 빌드 주의사항:**
> Next.js의 `output: 'standalone'`은 서버 코드만 포함한다.
> `.next/static` (JS/CSS 번들)과 `public` (정적 파일)은 **자동 포함되지 않으므로 반드시 별도 COPY** 해야 한다.
> 이걸 빠뜨리면 페이지는 뜨는데 CSS가 없거나 이미지가 안 보인다.

### 5.2 프론트엔드 .dockerignore

`aini-inu-frontend/.dockerignore`:

```
node_modules
.next
.env*
.git
.gitignore
.claude
*.md
npm-debug.log*
.DS_Store
```

> **`.dockerignore`란?**
> Docker 빌드 시 컨텍스트(빌드에 보내는 파일들)에서 제외할 패턴.
> `.gitignore`와 같은 문법. `node_modules`를 제외하면 빌드 컨텍스트 전송이 수초 → 수밀리초로 줄어든다.

### 5.3 Nginx 설정 파일

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

    # Swagger UI → backend (API 문서)
    location /swagger-ui/ {
        proxy_pass http://backend:8080;
    }
    location /v3/api-docs {
        proxy_pass http://backend:8080;
    }

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

> **Nginx 설정 해설:**
>
> **`limit_req_zone`** — 악의적인 요청 폭탄 방어. IP별로 초당 허용 요청 수를 제한한다.
> - `$binary_remote_addr` — 클라이언트 IP 주소 (바이너리 형태, 메모리 효율적)
> - `zone=api:10m` — "api"라는 이름의 10MB 공유 메모리 영역 (약 16만 IP 추적 가능)
> - `rate=10r/s` — 초당 10개 요청 허용
> - `burst=20` — 순간적으로 20개까지 허용 (초과 시 429 Too Many Requests)
> - `nodelay` — burst 범위 내 요청은 지연 없이 즉시 처리
>
> **`proxy_pass`** — 요청을 다른 서버로 전달 (리버스 프록시의 핵심).
> `http://backend:8080`에서 `backend`는 Docker Compose 서비스 이름 = Docker 내부 DNS로 자동 해석.
>
> **WebSocket 프록시 필수 헤더:**
> - `proxy_http_version 1.1` — WebSocket은 HTTP/1.1 필수
> - `Upgrade: websocket` — "이 연결을 WebSocket으로 업그레이드해줘"
> - `Connection: upgrade` — "연결을 유지하면서 프로토콜을 변경해줘"
> - 이 3줄이 없으면 WebSocket 연결이 즉시 끊어진다.
>
> **`proxy_read_timeout 3600s`** — Nginx는 기본 60초 동안 응답이 없으면 연결을 끊는다.
> WebSocket은 오래 유지되는 연결이므로 1시간(3600초)으로 늘림.
>
> **`client_max_body_size 10m`** — 요청 body 최대 크기. 기본값은 1MB라서
> 이미지 업로드 시 `413 Request Entity Too Large` 에러가 난다. 10MB로 설정.
>
> **`${DOMAIN}` 환경변수 치환:**
> nginx.conf를 `/etc/nginx/templates/default.conf.template`로 마운트하면
> Nginx 공식 Docker 이미지가 시작 시 `${DOMAIN}` 같은 환경변수를 자동 치환해준다.
> 이 덕분에 설정 파일에 도메인을 하드코딩하지 않아도 된다.

### 5.4 루트 docker-compose.yml

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

> **docker-compose.yml 용어 해설:**
>
> **`image: pgvector/pgvector:pg16`** — Docker Hub에서 이미지를 다운받아 사용.
> **`build: context: ./aini-inu-backend`** — 해당 디렉토리의 Dockerfile로 직접 빌드.
>
> **`restart: unless-stopped`** — 컨테이너가 죽으면 자동 재시작. 단, `docker compose stop`으로 수동 중지한 경우는 재시작 안 함.
>
> **`depends_on` + `condition: service_healthy`** — postgres가 healthcheck를 통과해야 backend가 시작됨.
> 이게 없으면 DB가 아직 준비 안 됐는데 backend가 먼저 시작해서 연결 실패.
>
> **`env_file: .env.docker`** — 이 파일의 환경변수를 컨테이너 안에 주입.
> 주의: docker compose 자체가 읽는 `.env`와는 별개. `.env`는 compose 파일 내 `${변수}` 치환용, `env_file`은 컨테이너 내부 환경변수 주입용.
>
> **`${POSTGRES_DB:-ainiinu}`** — `.env` 파일에 `POSTGRES_DB`가 있으면 그 값, 없으면 기본값 `ainiinu` 사용.
>
> **`volumes` (서비스 레벨):**
> - `aini_inu_postgres_data:/var/lib/postgresql/data` — DB 데이터를 Docker 볼륨에 영속 저장.
>   컨테이너가 삭제되어도 데이터는 유지됨.
> - `./nginx/nginx.conf:/etc/nginx/templates/default.conf.template:ro` — 호스트 파일을 컨테이너 안에 읽기전용(`:ro`)으로 마운트.
> - `/etc/letsencrypt:/etc/letsencrypt:ro` — 호스트의 SSL 인증서 디렉토리를 nginx에 마운트.
>
> **`ports: "80:80"`** — 호스트의 80포트 → 컨테이너의 80포트로 매핑. 외부에서 접속 가능.
> `ports`가 없는 서비스(postgres, backend, frontend)는 Docker 내부에서만 접근 가능.
>
> **`volumes` (최하단, 탑레벨):**
> 여기서 선언된 볼륨은 Docker가 관리하는 영속 저장소.
> `docker compose down`해도 유지됨. `docker compose down -v`하면 삭제됨 (주의!).

### 5.5 환경변수 파일

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

> **`.env` vs `.env.docker` vs `env_file`의 관계:**
>
> | 파일 | 누가 읽나 | 용도 |
> |------|----------|------|
> | `.env` | docker compose CLI | compose 파일 내 `${변수}` 치환 (예: `${DOMAIN}`, `${POSTGRES_DB}`) |
> | `.env.docker` | backend 컨테이너 | `env_file:` 지시어로 컨테이너 내부 환경변수 주입 (JWT_SECRET, GEMINI_API_KEY 등) |
>
> 우리 프로젝트에서는 둘의 내용이 동일하므로 `cp .env.docker .env`로 복사한다.
> `.env`를 만들지 않으면 `WARN: The "DOMAIN" variable is not set` 경고가 발생한다.

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

> **`nano` 편집기 기본 조작:**
> - 화살표 키로 이동, 그냥 타이핑하면 입력됨
> - `Ctrl+O` → `Enter` — 저장
> - `Ctrl+X` — 나가기
> - `Ctrl+K` — 현재 줄 잘라내기
> - `Ctrl+W` — 검색

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

> **certbot 옵션 설명:**
> - `certonly` — 인증서만 발급 (웹서버 설정 자동 수정 안 함)
> - `--standalone` — certbot이 임시 웹서버를 직접 띄워서 Let's Encrypt 검증 수행
> - `-d ainiinu.kr` — 인증서를 발급받을 도메인
> - `--agree-tos` — Let's Encrypt 이용약관 자동 동의
> - `-m` — 인증서 만료 알림을 받을 이메일

성공하면:
```
Successfully received certificate.
Certificate is saved at: /etc/letsencrypt/live/ainiinu.kr/fullchain.pem
Key is saved at:          /etc/letsencrypt/live/ainiinu.kr/privkey.pem
This certificate expires on 2026-06-07.
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

> **옵션 설명:**
> - `-d` (detached) — 백그라운드에서 실행. 이거 없으면 터미널에 로그가 계속 출력되고, 터미널 닫으면 서비스도 종료됨.
> - `--build` — 이미지를 다시 빌드. 코드 변경 후에는 반드시 필요. 변경 없으면 캐시 사용해서 빠름.

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

> **로그 옵션 설명:**
> - `-f` (follow) — 실시간으로 새 로그가 계속 출력됨. `tail -f`와 같은 개념.
> - `--tail=50` — 최근 50줄부터 보여줌. 이거 없으면 처음부터 전부 출력되어 스크롤 폭탄.
> - 로그 보다가 나가려면 `Ctrl+C`
> - **긴 로그가 화면을 넘어갈 때**: 스크롤이 아니라 `less` 형태의 페이저가 나올 수 있음 → `q`를 눌러서 나감

### 6.6 접속 확인

```
https://ainiinu.kr                      → 프론트엔드 (웹 페이지)
https://ainiinu.kr/api/v1/              → 백엔드 API
https://ainiinu.kr/swagger-ui/index.html → Swagger UI (API 문서)
wss://ainiinu.kr/ws/                    → WebSocket
```

포트번호 없이 도메인만으로 접속 가능 (nginx가 80/443 표준 포트 사용).

> **Swagger UI가 안 뜨고 프론트엔드 빈 페이지가 나올 때:**
>
> Nginx의 기본 라우팅(`location /`)이 모든 요청을 프론트엔드로 보내기 때문이다.
> `/swagger-ui/`와 `/v3/api-docs` 경로를 백엔드로 보내는 규칙을 nginx.conf에 추가해야 한다.
> 이처럼 **백엔드가 직접 서빙하는 경로가 추가되면 nginx.conf에도 라우팅 규칙을 추가**해야 한다.
>
> Springdoc OpenAPI가 사용하는 경로:
> - `/swagger-ui/*` — Swagger UI 웹 페이지 (HTML/JS/CSS)
> - `/v3/api-docs` — OpenAPI JSON 스펙 (Swagger UI가 내부적으로 호출)

> **왜 포트번호가 필요 없나?**
> HTTP의 기본 포트는 80, HTTPS의 기본 포트는 443이다.
> 브라우저는 `https://ainiinu.kr`을 `https://ainiinu.kr:443`으로 자동 해석한다.
> nginx가 443 포트를 점유하고 있으므로 포트 지정 없이 바로 접속 가능.

### 6.7 인증서 자동 갱신

certbot을 `sudo apt install`로 설치하면 systemd 타이머로 자동 갱신이 설정된다.
수동 갱신이 필요한 경우:

```bash
# nginx 중지 → 갱신 → nginx 재시작
docker compose stop nginx
sudo certbot renew
docker compose start nginx
```

> Let's Encrypt 인증서는 90일 유효. certbot은 만료 30일 전부터 자동 갱신을 시도한다.

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
| API 문서 (Swagger) | `https://ainiinu.kr/swagger-ui/index.html` — 브라우저에서 바로 확인 가능 |
| API 문서 (JSON) | `https://ainiinu.kr/v3/api-docs` — OpenAPI JSON 스펙 다운로드 |
| API 문서 (파일) | `common-docs/openapi/openapi.v1.json` — 레포 내 스냅샷 |
| 인증 | JWT 토큰 발급/갱신 흐름 동일하게 구현 |
| 이미지 URL | 백엔드 응답의 상대경로에 base URL 조합 필요 |
| WebSocket | `wss://ainiinu.kr/ws/chat-rooms/{roomId}` + STOMP + JWT 헤더 |
| 푸시 알림 | 추후 필요 시 백엔드에 APNs 연동 추가 |

---

## 9. 디버깅 가이드

### 9.1 502 Bad Gateway

브라우저에서 `502 Bad Gateway`가 뜨면 = **Nginx가 백엔드/프론트엔드에 연결을 못 하는 것**.

**디버깅 순서:**

```bash
# 1. 모든 서비스 상태 확인
docker compose ps
# → STATUS가 "Up"이 아닌 서비스가 있으면 그게 원인

# 2. 죽은 서비스의 로그 확인
docker compose logs --tail=100 backend   # 또는 frontend

# 3. 실시간 로그 보면서 요청 재시도
docker compose logs -f backend
# → 다른 브라우저 탭에서 회원가입 등 요청 시도
# → 로그에 에러가 찍히는지 확인

# 4. 백엔드 로그에 아무것도 안 찍히면 nginx 문제
docker compose logs --tail=50 nginx
```

**실제 겪은 사례:**

백엔드가 `Application run failed`로 기동 실패 → nginx가 backend:8080에 연결 불가 → 502 반환.
원인: 삭제된 SQL 시드 파일(`10_core_sample_seed.sql`)을 `application.properties`에서 여전히 참조.
해결: `spring.sql.init.data-locations`에서 해당 파일 참조 제거.

### 9.2 컨테이너가 계속 재시작될 때

```bash
# 재시작 횟수 확인
docker compose ps
# → RESTARTS 컬럼이 계속 증가하면 기동 실패 + 자동 재시작 반복 중

# 최근 로그에서 에러 찾기
docker compose logs --tail=200 backend | grep -i "error\|exception\|failed"
```

> **`grep -i` 란?** 대소문자 구분 없이(-i = ignore case) 패턴 검색.
> `|` (파이프) = 앞 명령의 출력을 뒤 명령의 입력으로 전달.
> `\|` = grep에서 OR 조건 ("error" 또는 "exception" 또는 "failed").

### 9.3 특정 서비스만 재빌드/재시작

```bash
# 백엔드만 재빌드 + 재시작 (다른 서비스는 건드리지 않음)
docker compose up -d --build backend

# nginx 설정 변경 후 nginx만 재시작
docker compose restart nginx

# 프론트엔드만 재빌드
docker compose up -d --build frontend
```

### 9.4 컨테이너 안에 들어가서 직접 확인

```bash
# backend 컨테이너 안에서 bash 실행
docker compose exec backend sh

# 컨테이너 안에서 환경변수 확인
env | grep SPRING
env | grep JWT

# 컨테이너 안에서 나가기
exit
```

> **`docker compose exec` vs `docker compose run`:**
> - `exec` — 이미 실행 중인 컨테이너 안에서 명령 실행
> - `run` — 새 컨테이너를 만들어서 명령 실행 (일회용)

### 9.5 DB 직접 접속

```bash
# postgres 컨테이너에서 psql 실행
docker compose exec postgres psql -U ainiinu -d ainiinu

# SQL 실행 예시
SELECT count(*) FROM member;
\dt                    -- 테이블 목록
\d member              -- member 테이블 구조
\q                     -- psql 종료
```

### 9.6 네트워크/포트 관련

```bash
# 80/443 포트를 누가 점유하고 있는지 확인
sudo ss -tlnp | grep ':80\|:443'

# Docker 내부 네트워크 확인 (컨테이너 간 통신 문제 시)
docker network ls
docker network inspect aini-inu-monorepo_default
```

> **`ss -tlnp` 설명:**
> - `ss` — socket statistics (네트워크 소켓 정보, `netstat`의 현대적 대체)
> - `-t` — TCP만
> - `-l` — LISTEN 상태만 (연결 대기 중인 포트)
> - `-n` — 숫자로 표시 (이름 해석 안 함, 빠름)
> - `-p` — 어떤 프로세스가 점유하는지 표시

### 9.7 디스크/리소스 관련

```bash
# 디스크 사용량 확인
df -h

# Docker가 사용하는 디스크 용량
docker system df

# 안 쓰는 이미지/컨테이너/볼륨 정리 (공간 확보)
docker system prune -f          # 중지된 컨테이너, 미사용 이미지 삭제
docker image prune -a -f        # 모든 미사용 이미지 삭제 (주의: 빌드 캐시도 삭제됨)
```

> **VM 디스크가 꽉 차면?** Docker 이미지와 빌드 캐시가 빠르게 쌓인다.
> `docker system df`로 확인하고, `docker system prune`으로 정리.
> 빌드할 때마다 이전 이미지가 남으므로 주기적으로 정리해야 한다.

---

## 10. 트러블슈팅 모음

### 10.1 Docker 설치 관련

**Q: `sudo apt install docker.io` 했는데 `docker compose` 명령이 없다?**

`docker.io`는 Debian/Ubuntu 기본 패키지로 Docker Compose 플러그인이 포함되지 않는다.
Docker 공식 저장소에서 `docker-ce` + `docker-compose-plugin`을 설치해야 한다. (4장 참고)

**Q: `Unable to locate package docker-compose-plugin`?**

Docker 공식 저장소가 시스템에 등록되지 않은 상태. 4.3절의 GPG 키 + 저장소 추가 과정을 먼저 수행해야 한다.

**Q: `Package 'docker-ce' has no installation candidate`?**

Ubuntu용 저장소를 추가했는데 실제 OS가 Debian인 경우. `lsb_release -cs`로 OS를 확인하고, 결과가 `bookworm`이면 Debian 12이므로 저장소 URL을 `https://download.docker.com/linux/debian`으로 변경해야 한다.

**Q: Docker Desktop을 설치하면 안 되나?**

GCP VM은 GUI가 없는 서버이므로 Docker Desktop은 설치할 수 없다. Docker Desktop은 Mac/Windows 데스크탑 환경 전용이다.

### 10.2 비공개 레포 clone 관련

**Q: Organization 비공개 레포를 clone하면 권한 에러가 난다?**

GitHub PAT (Personal Access Token)을 생성하고, Organization access에서 해당 org에 **"Grant"** 버튼을 클릭해야 한다. 개인 토큰을 만들었다고 자동으로 org 레포에 접근할 수 있는 것이 아니다.

### 10.3 환경변수 관련

**Q: `WARN: The "DOMAIN" variable is not set. Defaulting to a blank string.`?**

docker compose는 기본적으로 프로젝트 루트의 `.env` 파일을 읽는다. `.env.docker`만 만들고 `.env`를 만들지 않으면 이 경고가 발생한다.

```bash
cp .env.docker .env
```

### 10.4 SSL 인증서 관련

**Q: `docker compose run --rm certbot ...` 이 Creating에서 멈춘다?**

Docker certbot 컨테이너 방식은 여러 이유로 멈출 수 있다. 호스트에 직접 certbot을 설치하는 것이 가장 확실하다:

```bash
docker compose down              # 80 포트 해제
sudo apt install -y certbot      # 호스트에 직접 설치
sudo certbot certonly --standalone -d ainiinu.kr --agree-tos -m your@email.com
```

**Q: certbot standalone도 멈춘다?**

GCP 방화벽에서 TCP 80 포트가 열려있는지 확인. Let's Encrypt 서버가 `http://도메인:80`으로 검증 요청을 보내는데, 방화벽에서 차단되면 무한 대기한다.

### 10.5 디스크 용량 부족 (`no space left on device`)

**증상:**
Docker 빌드 중 에러 발생:
```
target backend: failed to solve: ResourceExhausted: failed to copy files:
copy file range failed: no space left on device
```

**원인:**
GCP 기본 디스크가 10GB인데, Gradle 빌드(JDK + 의존성)와 Next.js 빌드(node_modules)가 동시에 진행되면 디스크가 가득 참.

**즉시 해결 — Docker 캐시 정리:**
```bash
# 미사용 이미지, 빌드 캐시, 중지된 컨테이너 전부 삭제
docker system prune -a -f

# 정리된 용량 + 남은 용량 확인
df -h
```

> **`docker system prune -a -f` 설명:**
> - `prune` — 미사용 리소스 정리
> - `-a` (all) — 현재 실행 중이 아닌 모든 이미지도 삭제 (태그된 이미지 포함)
> - `-f` (force) — 확인 질문 없이 바로 삭제
> - 주의: 다음 `docker compose up --build` 시 처음부터 다시 빌드하므로 시간이 더 걸림

**근본 해결 — 디스크 크기 늘리기:**

1. GCP 콘솔 → **Compute Engine** → **디스크** 메뉴
2. VM에 연결된 디스크 클릭 → 상단 **수정** 버튼
3. 크기를 **20GB** 이상으로 변경 → **저장**
4. VM에서 파티션 확장 (VM 재시작 없이 가능):

```bash
# 파일시스템 확장 (ext4 기준)
sudo resize2fs /dev/sda1

# 확장 확인
df -h
# → /dev/sda1 의 Size가 늘어났는지 확인
```

> **`resize2fs`란?** ext4 파일시스템의 크기를 디스크에 맞게 확장하는 명령어.
> GCP에서 디스크 크기를 늘려도 OS가 자동으로 인식하지 않으므로 이 명령이 필요하다.
> 데이터 손실 없이 온라인(서비스 중단 없이) 확장 가능.

**디스크 용량 모니터링:**

```bash
# 전체 디스크 사용량
df -h

# Docker가 사용하는 용량 상세
docker system df

# 어떤 디렉토리가 용량을 많이 차지하는지 확인
sudo du -sh /* 2>/dev/null | sort -rh | head -10
```

> **`df -h` 읽는 법:**
> ```
> Filesystem  Size  Used  Avail  Use%  Mounted on
> /dev/sda1    20G  3.8G   15G   21%   /
> ```
> - `Size` — 전체 디스크 크기
> - `Used` — 사용 중인 용량
> - `Avail` — 남은 용량
> - `Use%` — 사용률 (**80% 넘으면 주의, 90% 넘으면 위험**)
> - `Mounted on` — `/`가 루트 파티션 (가장 중요)

**빌드별 예상 디스크 사용량:**

| 항목 | 용량 |
|------|------|
| OS + 시스템 패키지 | ~2GB |
| Docker 엔진 | ~500MB |
| 백엔드 빌드 이미지 (Gradle + JDK) | ~2.5GB |
| 프론트엔드 빌드 이미지 (Node.js + npm) | ~1.5GB |
| 최종 실행 이미지 (backend + frontend + nginx + postgres) | ~1.5GB |
| PostgreSQL 데이터 | ~500MB (데이터 양에 따라 증가) |
| 업로드 이미지 (사용자 데이터) | 가변 |
| **합계 (여유 포함)** | **최소 15GB, 권장 20GB** |

### 10.6 Swagger UI가 안 뜨고 프론트엔드 페이지가 나옴

**증상:** `https://ainiinu.kr/swagger-ui/index.html` 접속 시 Swagger가 아니라 프론트엔드 빈 페이지가 나옴.

**원인:** Nginx의 `location /`이 매칭되지 않는 모든 경로를 프론트엔드로 보냄. `/swagger-ui/`도 프론트엔드로 가버림.

**해결:** nginx.conf에 Swagger 경로를 백엔드로 라우팅하는 규칙 추가:

```nginx
location /swagger-ui/ {
    proxy_pass http://backend:8080;
}
location /v3/api-docs {
    proxy_pass http://backend:8080;
}
```

> **교훈:** 백엔드가 직접 서빙하는 경로가 새로 추가되면, nginx.conf에도 해당 경로의 라우팅 규칙을 반드시 추가해야 한다.
> Nginx는 가장 구체적인(longest prefix) `location`부터 매칭하므로, `/swagger-ui/`는 `/`보다 우선 매칭된다.

### 10.7 백엔드 기동 실패

**Q: `No data scripts found at location 'classpath:db/seed/xxx.sql'`?**

`application.properties`의 `spring.sql.init.data-locations`에서 참조하는 SQL 파일이 실제로 존재하지 않는 경우.
파일이 삭제/이동되었으면 설정에서도 해당 참조를 제거해야 한다.

**Q: 백엔드 로그에서 `Application run failed`가 보인다?**

Spring Boot가 시작에 실패한 것. 로그에서 `Caused by:` 또는 `Error`를 찾으면 근본 원인이 나온다:

```bash
docker compose logs backend | grep -i "caused by\|error creating bean\|application run failed"
```

---

## 11. HTTPS 방식 비교 (참고)

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

## 12. 리눅스/터미널 필수 명령어

GCP VM에 SSH로 접속하면 터미널(CLI)만 사용할 수 있다.
자주 쓰는 명령어 정리:

### 12.1 파일/디렉토리

```bash
ls                 # 현재 디렉토리 파일 목록
ls -la             # 숨김 파일 포함, 상세 정보 (권한, 크기, 날짜)
pwd                # 현재 위치 (Print Working Directory)
cd /path/to/dir    # 디렉토리 이동
cd ..              # 상위 디렉토리
cd ~               # 홈 디렉토리

cat filename       # 파일 내용 전체 출력
less filename      # 파일 내용을 페이지 단위로 보기 (q로 나감)
head -n 20 file    # 처음 20줄만 보기
tail -n 20 file    # 마지막 20줄만 보기
tail -f file       # 파일에 추가되는 내용을 실시간으로 보기 (로그 모니터링)
```

### 12.2 파일 편집

```bash
nano filename      # nano 편집기 (초보자용, 하단에 단축키 표시됨)
vi filename        # vi 편집기 (익숙해지면 빠름)
```

> **nano 단축키:**
> `Ctrl+O` → `Enter` = 저장, `Ctrl+X` = 나가기, `Ctrl+K` = 줄 삭제, `Ctrl+W` = 검색
>
> **vi 최소 생존 가이드:**
> - `i` → 입력 모드 (글자 입력 가능)
> - `Esc` → 명령 모드 (입력 끝)
> - `:wq` + `Enter` → 저장 후 종료
> - `:q!` + `Enter` → 저장 안 하고 종료
> - `/검색어` + `Enter` → 검색, `n`으로 다음 결과

### 12.3 프로세스/시스템

```bash
ps aux                   # 실행 중인 모든 프로세스
ps aux | grep docker     # docker 관련 프로세스만 필터
kill <PID>               # 프로세스 종료
sudo systemctl status docker  # Docker 서비스 상태 확인
```

### 12.4 네트워크

```bash
curl https://ainiinu.kr              # URL에 HTTP 요청 보내기
curl -I https://ainiinu.kr           # 응답 헤더만 보기
curl -s ifconfig.me                  # 내 외부 IP 확인
nslookup ainiinu.kr                  # DNS 조회
sudo ss -tlnp                        # 열려있는 포트 확인
```

### 12.5 터미널 제어

```bash
Ctrl+C    # 실행 중인 명령 강제 중지 (로그 보기, 서버 실행 등을 멈출 때)
Ctrl+D    # 입력 종료 / 터미널 세션 종료
Ctrl+L    # 화면 지우기 (clear와 동일)
q         # less, git log 등 페이저에서 나가기
↑/↓       # 이전에 입력한 명령어 히스토리 탐색
Tab       # 파일명/명령어 자동완성
```

> **`q`로 나가는 상황들:**
> - `docker compose logs` (페이저 모드일 때)
> - `git log` (커밋 이력이 길 때)
> - `less` 명령으로 파일 볼 때
> - `man` 명령으로 매뉴얼 볼 때
>
> 화면에 `:` 또는 `(END)`가 보이면 `q`를 눌러 나가면 된다.
> `Ctrl+C`가 안 먹히고 화면이 멈춰보이면 거의 `q`로 나갈 수 있다.

### 12.6 유용한 조합

```bash
# 파이프 (|) — 앞 명령의 출력을 뒤 명령의 입력으로 전달
docker compose logs backend | grep ERROR     # 로그에서 ERROR만 필터
ps aux | grep nginx                          # nginx 프로세스 찾기

# && — 앞 명령 성공 시에만 뒤 명령 실행
git pull && docker compose up -d --build     # pull 성공해야 빌드 시작

# ; — 앞 명령 결과와 무관하게 뒤 명령도 실행
docker compose down; docker compose up -d    # 항상 둘 다 실행

# > — 출력을 파일로 저장 (덮어쓰기)
docker compose logs backend > backend.log    # 로그를 파일로 저장

# >> — 출력을 파일에 추가 (이어쓰기)
echo "메모" >> notes.txt
```

---

## 13. 운영 명령어 치트시트

### 13.1 일상 운영

```bash
# 전체 서비스 시작 (빌드 포함)
docker compose up -d --build

# 전체 서비스 중지
docker compose down

# 서비스 상태 확인
docker compose ps

# 코드 업데이트 후 재배포
git pull && docker compose up -d --build
```

### 13.2 로그 확인

```bash
# 전체 로그 (최근 50줄부터 실시간)
docker compose logs -f --tail=50

# 특정 서비스 로그
docker compose logs -f backend
docker compose logs -f nginx
docker compose logs -f frontend
docker compose logs -f postgres

# 로그에서 에러만 찾기
docker compose logs backend | grep -i error
```

### 13.3 서비스 관리

```bash
# 특정 서비스만 재시작
docker compose restart backend
docker compose restart nginx

# 특정 서비스만 재빌드
docker compose up -d --build backend
docker compose up -d --build frontend
```

### 13.4 SSL 인증서

```bash
# 인증서 만료일 확인
sudo certbot certificates

# 수동 갱신
docker compose stop nginx
sudo certbot renew
docker compose start nginx
```

### 13.5 데이터 관리

```bash
# DB 데이터는 유지하면서 컨테이너만 재생성
docker compose down
docker compose up -d --build

# 모든 것을 초기화 (DB 데이터 포함 삭제 — 매우 주의!)
docker compose down -v
```

### 13.6 디스크 정리

```bash
# Docker 디스크 사용량 확인
docker system df

# 미사용 리소스 정리
docker system prune -f

# 모든 미사용 이미지 삭제 (빌드 캐시 포함 — 다음 빌드 느려짐)
docker image prune -a -f
```

---

## 14. 결론

- 현재 소스코드에서 **코드 수정 최소** (next.config.ts에 `output: 'standalone'` 한 줄)로 Docker 배포 가능
- 프로젝트에 추가한 파일: 프론트엔드 Dockerfile, .dockerignore, nginx.conf, 루트 docker-compose.yml, .env.docker.example
- GCP VM(Debian 12)에 Docker 공식 저장소에서 설치, SSL은 호스트 certbot으로 발급
- `docker compose up -d --build`로 전체 서비스 기동
- `https://ainiinu.kr`로 웹과 iOS 모두 동일한 도메인으로 접속
