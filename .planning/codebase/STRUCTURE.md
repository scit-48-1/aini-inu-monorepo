# Codebase Structure

**Analysis Date:** 2026-03-06

## Directory Layout

```
aini-inu/                                  # Monorepo root
├── aini-inu-backend/                      # Spring Boot API server
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/scit/ainiinu/         # Root package
│   │   │   │   ├── AiniInuApplication.java
│   │   │   │   ├── common/                # Cross-cutting (auth, errors, config)
│   │   │   │   ├── member/                # Member domain
│   │   │   │   ├── pet/                   # Pet domain
│   │   │   │   ├── walk/                  # Walk thread + diary domain
│   │   │   │   ├── chat/                  # Chat domain (HTTP + WebSocket)
│   │   │   │   ├── community/             # Community posts domain
│   │   │   │   └── lostpet/               # Lost pet AI matching domain
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── db/
│   │   │           ├── ddl/               # Schema alignment scripts
│   │   │           └── seed/              # Sample/lookup data
│   │   └── test/
│   │       ├── java/scit/ainiinu/         # Test mirror structure
│   │       └── resources/                 # Test-specific config
│   ├── scripts/                           # DevOps helper scripts
│   ├── docker/                            # Docker config
│   ├── docker-compose.yml
│   ├── Dockerfile
│   ├── build.gradle
│   └── settings.gradle
│
├── aini-inu-frontend/                     # Next.js client app
│   ├── src/
│   │   ├── app/                           # Next.js App Router pages
│   │   │   ├── layout.tsx                 # Root layout (auth guard, sidebar, providers)
│   │   │   ├── page.tsx                   # Landing page
│   │   │   ├── globals.css                # Global Tailwind styles
│   │   │   ├── api/check-key/             # API route (server-side)
│   │   │   ├── login/                     # Login page
│   │   │   ├── signup/                    # Signup page
│   │   │   ├── dashboard/                 # Dashboard page
│   │   │   ├── feed/                      # Community feed page
│   │   │   ├── chat/                      # Chat pages (list + [id] detail)
│   │   │   ├── around-me/                 # Walk radar/map page
│   │   │   ├── profile/                   # Profile pages (own + [memberId])
│   │   │   └── settings/                  # Settings page
│   │   ├── components/                    # UI components by feature
│   │   │   ├── around-me/                 # Radar, sidebar, forms
│   │   │   ├── chat/                      # Chat UI components
│   │   │   ├── common/                    # Sidebar, ThemeProvider, modals, map
│   │   │   ├── dashboard/                 # Dashboard widgets
│   │   │   ├── feed/                      # Feed components
│   │   │   ├── profile/                   # Profile sections and modals
│   │   │   ├── shared/                    # Shared forms, modals
│   │   │   ├── signup/                    # Multi-step signup wizard
│   │   │   └── ui/                        # Design system primitives
│   │   ├── services/                      # API communication layer
│   │   │   ├── api/                       # apiClient + domain services
│   │   │   ├── authService.ts             # Auth/login service
│   │   │   └── geminiService.ts           # Client-side Gemini AI
│   │   ├── store/                         # Zustand state stores
│   │   ├── hooks/                         # Custom React hooks
│   │   │   └── forms/                     # Form-specific hooks
│   │   ├── types/                         # TypeScript type definitions
│   │   ├── constants/                     # Static data (breeds, users, etc.)
│   │   ├── lib/                           # Utility functions
│   │   └── mocks/                         # MSW mock handlers
│   ├── public/                            # Static assets
│   │   └── images/                        # Image assets (symlinked from common-docs)
│   ├── next.config.ts
│   ├── tsconfig.json
│   ├── eslint.config.mjs
│   ├── postcss.config.mjs
│   └── package.json
│
├── common-docs/                           # Shared contract artifacts
│   ├── PROJECT_PRD.md                     # Product requirements (source of truth)
│   ├── openapi/
│   │   ├── openapi.v1.json                # API contract snapshot
│   │   └── README.md
│   └── images/                            # Shared image assets
│
├── CLAUDE.md                              # AI assistant instructions
├── .gitignore
├── .claude/                               # Claude Code config
│   ├── commands/gsd/                      # GSD workflow commands
│   └── get-shit-done/                     # GSD framework
├── .agents/skills/                        # Agent skill definitions
└── .planning/                             # Planning documents
    └── codebase/                          # Architecture/analysis docs
```

## Directory Purposes

**`aini-inu-backend/src/main/java/scit/ainiinu/common/`:**
- Purpose: Shared infrastructure code used by all domains
- Contains: Security (JWT, auth annotations), exception handling, response wrappers, entity base classes, configuration
- Key files:
  - `config/SecurityConfig.java` - Disables Spring Security defaults
  - `config/WebConfig.java` - Registers JWT interceptor, `@CurrentMember` resolver, CORS
  - `config/JpaConfig.java` - JPA auditing enablement
  - `config/OpenApiConfig.java` - Swagger/OpenAPI configuration
  - `exception/GlobalExceptionHandler.java` - Centralized `@RestControllerAdvice`
  - `exception/ErrorCode.java` - Interface all domain error codes implement
  - `exception/BusinessException.java` - Base domain exception
  - `exception/CommonErrorCode.java` - Shared error codes (C001-C999)
  - `response/ApiResponse.java` - Unified response envelope
  - `response/SliceResponse.java` - Infinite scroll pagination wrapper
  - `response/CursorResponse.java` - Cursor-based pagination wrapper
  - `entity/BaseTimeEntity.java` - `createdAt`/`updatedAt` auditing superclass
  - `entity/vo/Location.java` - Shared lat/lng value object
  - `security/annotation/CurrentMember.java` - Auth parameter injection
  - `security/annotation/Public.java` - Skip-auth marker
  - `security/interceptor/JwtAuthInterceptor.java` - JWT validation
  - `security/resolver/CurrentMemberArgumentResolver.java` - Resolves `@CurrentMember`
  - `security/jwt/JwtTokenProvider.java` - JWT token operations
  - `security/controller/TestAuthController.java` - Dev-only token generation

**Each backend domain (`member/`, `pet/`, `walk/`, `chat/`, `community/`, `lostpet/`):**
- Purpose: Self-contained domain module
- Contains: controller, service, repository, entity (+ enums, vo), dto (request + response), exception (ErrorCode enum + optional Exception class)
- Naming: `{Domain}Controller.java`, `{Domain}Service.java`, `{Domain}Repository.java`, `{Domain}ErrorCode.java`

**`aini-inu-backend/src/main/java/scit/ainiinu/lostpet/integration/`:**
- Purpose: External integration adapters specific to the lostpet domain
- Contains:
  - `ai/LostPetAiClient.java` (interface), `ai/LostPetAiClientImpl.java` - Spring AI / Gemini embeddings
  - `chat/ChatRoomDirectClient.java` (interface), `chat/ChatRoomDirectClientImpl.java` - Internal HTTP to chat service
- Pattern: Interface + Impl with Resilience4j retry on chat client

**`aini-inu-backend/src/main/java/scit/ainiinu/chat/realtime/`:**
- Purpose: WebSocket real-time messaging abstraction
- Contains:
  - `ChatRealtimePublisher.java` (interface) - publish events to chat rooms
  - `StompChatRealtimePublisher.java` - STOMP implementation
  - `ChatRealtimeEvent.java` - Generic event wrapper with `type` and `data`
  - `ChatRealtimeEventHandler.java` - Event handling

**`aini-inu-backend/src/main/resources/db/ddl/`:**
- Purpose: Incremental schema alignment SQL scripts (indexes, constraints, cleanup)
- Contains: Numbered SQL files (`01_walk_indexes_constraints.sql` through `06_legacy_story_cleanup.sql`)
- Loaded: Automatically on startup via `spring.sql.init.schema-locations`

**`aini-inu-backend/src/main/resources/db/seed/`:**
- Purpose: Lookup tables and sample data for development
- Contains: `00_lookup_seed.sql`, `10_core_sample_seed.sql`, `20_status_edge_seed.sql`, `99_reset_sequences.sql`
- Loaded: Automatically on startup (disabled in test profile)

**`aini-inu-frontend/src/components/ui/`:**
- Purpose: Design system primitives (pure presentation, no business logic)
- Contains: `Badge.tsx`, `Button.tsx`, `Card.tsx`, `Typography.tsx`

**`aini-inu-frontend/src/components/common/`:**
- Purpose: App-wide shared components
- Contains: `Sidebar.tsx`, `ThemeProvider.tsx`, `DynamicMap.tsx`, `ConfirmModal.tsx`, `CreatePostModal.tsx`, `MannerScoreGauge.tsx`, `UserAvatar.tsx`, `BookFlip/`

**`aini-inu-frontend/src/hooks/`:**
- Purpose: Custom React hooks for data fetching and state management
- Contains:
  - `useProfile.ts` - Wrapper around `useUserStore` for profile data
  - `useMemberProfile.ts` - Fetch other member profiles
  - `useMyDogs.ts` - Fetch current user's dogs
  - `useFollowToggle.ts` - Follow/unfollow logic
  - `useWalkDiaries.ts` - Walk diary data
  - `useRadarLogic.ts` - Walk radar/map logic
  - `forms/useSignupForm.ts`, `forms/useDogForm.ts`, `forms/usePostForm.ts`, `forms/useDiaryForm.ts` - Form state management

**`aini-inu-frontend/src/mocks/`:**
- Purpose: MSW (Mock Service Worker) for offline frontend development
- Contains: `browser.ts` (worker setup), `handlers.ts` (mock API handlers), `MSWProvider.tsx` (React wrapper)
- Key: `handlers.ts` seeds a full in-memory database via `localStorage` key `aini_inu_v6_db`

**`aini-inu-frontend/src/constants/`:**
- Purpose: Static reference data and mock fixtures
- Contains: `dogBreeds.ts`, `dogPersonalities.ts`, `portraits.ts`, `users.ts`, `index.ts` (barrel)

**`common-docs/`:**
- Purpose: Shared artifacts between frontend and backend teams
- Contains:
  - `PROJECT_PRD.md` - Product requirements document (highest priority source of truth)
  - `openapi/openapi.v1.json` - API contract snapshot (second priority source of truth)
  - `images/` - Shared image assets (source of truth, symlinked to frontend `public/`)

## Key File Locations

**Entry Points:**
- `aini-inu-backend/src/main/java/scit/ainiinu/AiniInuApplication.java`: Backend bootstrap
- `aini-inu-frontend/src/app/layout.tsx`: Frontend root layout (providers, auth guard, sidebar)
- `aini-inu-frontend/src/app/page.tsx`: Landing page

**Configuration:**
- `aini-inu-backend/build.gradle`: Dependencies and build config
- `aini-inu-backend/src/main/resources/application.properties`: All backend config (DB, JWT, AI, storage)
- `aini-inu-frontend/next.config.ts`: Next.js config (image remote patterns)
- `aini-inu-frontend/tsconfig.json`: TypeScript config with `@/*` path alias
- `aini-inu-frontend/eslint.config.mjs`: ESLint config
- `aini-inu-frontend/postcss.config.mjs`: PostCSS/Tailwind config

**Core Logic:**
- `aini-inu-backend/.../common/security/interceptor/JwtAuthInterceptor.java`: Authentication pipeline
- `aini-inu-backend/.../common/exception/GlobalExceptionHandler.java`: Error handling pipeline
- `aini-inu-backend/.../common/response/ApiResponse.java`: Response envelope
- `aini-inu-frontend/src/services/api/apiClient.ts`: Frontend HTTP client

**Type Definitions:**
- `aini-inu-frontend/src/types/index.ts`: All frontend type definitions (`UserType`, `DogType`, `ThreadType`, `ChatRoom`, `FeedPostType`, etc.)

**Testing:**
- `aini-inu-backend/src/test/java/scit/ainiinu/`: All backend tests mirror main structure
- `aini-inu-backend/src/test/java/scit/ainiinu/testsupport/`: Shared test utilities

**Database Schema:**
- `aini-inu-backend/src/main/resources/db/ddl/`: Schema alignment scripts
- `aini-inu-backend/src/main/resources/db/seed/`: Seed data

**Docker/DevOps:**
- `aini-inu-backend/docker-compose.yml`: PostgreSQL + backend compose
- `aini-inu-backend/Dockerfile`: Backend container
- `aini-inu-backend/scripts/docker-up.sh`, `docker-down.sh`, `docker-logs.sh`: Docker helpers
- `aini-inu-backend/scripts/export-openapi.sh`: OpenAPI export to common-docs

## Naming Conventions

**Files (Backend):**
- Controllers: `{Domain}Controller.java` (e.g., `MemberController.java`, `PostController.java`)
- Services: `{Domain}Service.java` or `{Domain}{Qualifier}Service.java` (e.g., `LostPetAnalyzeService.java`)
- Repositories: `{Entity}Repository.java` (e.g., `WalkDiaryRepository.java`)
- Entities: PascalCase matching table concept (e.g., `WalkDiary.java`, `ChatRoom.java`)
- DTOs: `{Entity}{Action}Request.java` / `{Entity}{Qualifier}Response.java` (e.g., `WalkDiaryCreateRequest.java`, `ThreadSummaryResponse.java`)
- Error codes: `{Domain}ErrorCode.java` (e.g., `ChatErrorCode.java`)
- Exceptions: `{Domain}Exception.java` (e.g., `ChatException.java`) - some domains have these, others only use `BusinessException`

**Files (Frontend):**
- Pages: `page.tsx` inside route directories
- Components: PascalCase (e.g., `ChatHeader.tsx`, `ProfileDogs.tsx`)
- Hooks: `use{Name}.ts` (e.g., `useProfile.ts`, `useRadarLogic.ts`)
- Services: `{domain}Service.ts` (e.g., `memberService.ts`, `chatService.ts`)
- Stores: `use{Name}Store.ts` (e.g., `useUserStore.ts`, `useConfigStore.ts`)
- Constants: camelCase (e.g., `dogBreeds.ts`, `portraits.ts`)

**Directories (Backend):**
- Domain packages: lowercase singular (`member/`, `pet/`, `walk/`, `chat/`, `community/`, `lostpet/`)
- Sub-packages: `controller/`, `service/`, `repository/`, `entity/`, `dto/request/`, `dto/response/`, `exception/`
- Special: `config/` (per-domain configuration), `integration/` (external adapters, lostpet only), `realtime/` (WebSocket, chat only), `api/` (empty, chat)

**Directories (Frontend):**
- Component groups: kebab-case matching route or feature (`around-me/`, `chat/`, `profile/`)
- Shared: `common/` (app-wide), `shared/` (cross-feature), `ui/` (design primitives)

## Where to Add New Code

**New Backend Domain:**
1. Create package: `aini-inu-backend/src/main/java/scit/ainiinu/{newdomain}/`
2. Add sub-packages: `controller/`, `service/`, `repository/`, `entity/`, `dto/request/`, `dto/response/`, `exception/`
3. Create `{NewDomain}ErrorCode.java` implementing `ErrorCode` interface
4. Controller: `@RestController`, `@RequestMapping("/api/v1/{resource}")`, inject service via `@RequiredArgsConstructor`
5. Service: `@Service`, `@Transactional(readOnly = true)` at class level
6. Entity: extend `BaseTimeEntity`, use `@GeneratedValue(strategy = GenerationType.IDENTITY)`
7. Add tests mirroring main structure under `src/test/java/scit/ainiinu/{newdomain}/`
8. Update OpenAPI snapshot: `./scripts/export-openapi.sh`

**New Backend Endpoint (Existing Domain):**
1. Add DTO in `dto/request/` and/or `dto/response/`
2. Add service method
3. Add controller method with `@Operation` annotation, `@CurrentMember Long memberId` parameter
4. Wrap return in `ResponseEntity.ok(ApiResponse.success(result))`
5. Add test(s) matching naming convention (`*Test`, `*SliceTest`, `*ContractTest`, `*IntegrationTest`)

**New Frontend Page:**
1. Create directory: `aini-inu-frontend/src/app/{route}/`
2. Add `page.tsx` with `'use client'` directive
3. If authenticated: page is auto-guarded by `layout.tsx` (add path to `PROTECTED_PATHS` if not already covered)
4. Create components in `aini-inu-frontend/src/components/{route}/`

**New Frontend Component:**
- Feature-specific: `aini-inu-frontend/src/components/{feature}/{ComponentName}.tsx`
- Shared across features: `aini-inu-frontend/src/components/shared/{ComponentName}.tsx`
- Design primitive: `aini-inu-frontend/src/components/ui/{ComponentName}.tsx`
- App-wide: `aini-inu-frontend/src/components/common/{ComponentName}.tsx`

**New Frontend API Service:**
1. Add service functions in `aini-inu-frontend/src/services/api/{domain}Service.ts`
2. Use `apiClient.get<T>('/endpoint')` pattern
3. Add TypeScript types in `aini-inu-frontend/src/types/index.ts`

**New Frontend Hook:**
- Data-fetching: `aini-inu-frontend/src/hooks/use{Feature}.ts`
- Form: `aini-inu-frontend/src/hooks/forms/use{Feature}Form.ts`

**New Frontend Zustand Store:**
- `aini-inu-frontend/src/store/use{Name}Store.ts`
- Pattern: `create<StateType>((set, get) => ({...}))`

**Utilities:**
- Backend: `aini-inu-backend/src/main/java/scit/ainiinu/common/` (appropriate sub-package)
- Frontend: `aini-inu-frontend/src/lib/{utilName}.ts`

## Special Directories

**`aini-inu-backend/src/main/resources/db/`:**
- Purpose: SQL scripts for schema and seed data
- Generated: No (hand-written)
- Committed: Yes
- Note: DDL scripts run on every startup (`spring.sql.init.mode=always`); test profile disables them

**`aini-inu-frontend/src/mocks/`:**
- Purpose: MSW mock layer for offline frontend development
- Generated: No (hand-written mock handlers)
- Committed: Yes
- Note: Active only in development when MSW is enabled; seeds localStorage with full mock database

**`aini-inu-frontend/public/images/`:**
- Purpose: Image assets for frontend
- Generated: No
- Committed: Symlinked from `common-docs/images/`

**`aini-inu-frontend/.next/`:**
- Purpose: Next.js build output
- Generated: Yes
- Committed: No (in `.gitignore`)

**`aini-inu-backend/build/`:**
- Purpose: Gradle build output
- Generated: Yes
- Committed: No (in `.gitignore`)

**`common-docs/openapi/`:**
- Purpose: API contract snapshot
- Generated: Via `scripts/export-openapi.sh` from running backend
- Committed: Yes (serves as contract artifact)

**`.planning/`:**
- Purpose: Architecture analysis and planning documents
- Generated: By analysis tools
- Committed: Yes

**`aini-inu-backend/src/test/java/scit/ainiinu/testsupport/`:**
- Purpose: Shared test utilities (annotations, builders, helpers)
- Generated: No
- Committed: Yes

---

*Structure analysis: 2026-03-06*
