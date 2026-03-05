# Code Conventions Map

## Scope
- This repository is a monorepo-style workspace with backend at `aini-inu-backend` and frontend at `aini-inu-frontend`.
- Conventions below are inferred from live code, not from a standalone style guide.

## Backend Architecture and Organization
- Backend package root is `aini-inu-backend/src/main/java/scit/ainiinu`.
- Primary organization is domain-first by context: `chat`, `community`, `lostpet`, `member`, `pet`, `walk`, plus shared `common`.
- Most domains keep consistent layer folders: `controller`, `service`, `repository`, `dto`, `entity` or `domain`, `exception/error`.
- Shared cross-cutting code lives in `aini-inu-backend/src/main/java/scit/ainiinu/common/*`.

## API and Controller Patterns
- REST endpoints are versioned under `/api/v1` via controllers like `aini-inu-backend/src/main/java/scit/ainiinu/pet/controller/PetController.java`.
- Controllers generally return `ResponseEntity<ApiResponse<T>>` instead of raw DTOs.
- API wrapper convention is centralized in `aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java`.
- Slice and cursor pagination wrappers are standardized via `aini-inu-backend/src/main/java/scit/ainiinu/common/response/SliceResponse.java` and `aini-inu-backend/src/main/java/scit/ainiinu/common/response/CursorResponse.java`.
- OpenAPI annotations are used heavily (`@Operation`, `@Tag`, `@SecurityRequirement`) in controllers such as `aini-inu-backend/src/main/java/scit/ainiinu/community/controller/PostController.java`.

## Security and Auth Conventions
- Authentication is interceptor-based rather than standard Spring Security auth chain.
- Security baseline is in `aini-inu-backend/src/main/java/scit/ainiinu/common/config/SecurityConfig.java` (permit-all with defaults disabled).
- JWT enforcement is done by `aini-inu-backend/src/main/java/scit/ainiinu/common/security/interceptor/JwtAuthInterceptor.java`.
- Authenticated member ID injection uses `@CurrentMember` from `aini-inu-backend/src/main/java/scit/ainiinu/common/security/annotation/CurrentMember.java` and resolver `aini-inu-backend/src/main/java/scit/ainiinu/common/security/resolver/CurrentMemberArgumentResolver.java`.
- Public endpoints are explicitly marked with `@Public` in `aini-inu-backend/src/main/java/scit/ainiinu/common/security/annotation/Public.java`.

## Service and Transaction Patterns
- Services commonly use constructor injection via Lombok `@RequiredArgsConstructor`.
- Services default to class-level `@Transactional(readOnly = true)` and opt into write methods with method-level `@Transactional`, e.g. `aini-inu-backend/src/main/java/scit/ainiinu/pet/service/PetService.java`.
- Business rules and ownership checks are enforced in service methods before repository writes.

## Error Handling and Domain Error Codes
- Error abstraction is `ErrorCode` interface in `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/ErrorCode.java`.
- Shared error enum is `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/CommonErrorCode.java`.
- Domain-specific enums like `aini-inu-backend/src/main/java/scit/ainiinu/member/exception/MemberErrorCode.java` follow the same contract.
- Runtime domain failures use `BusinessException` from `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/BusinessException.java`.
- Global mapping to API payloads is centralized in `aini-inu-backend/src/main/java/scit/ainiinu/common/exception/GlobalExceptionHandler.java`.

## DTO, Validation, and Serialization Style
- Request DTOs often use mutable Lombok classes (`@Getter/@Setter/@NoArgsConstructor`) with Bean Validation annotations, e.g. `aini-inu-backend/src/main/java/scit/ainiinu/pet/dto/request/PetCreateRequest.java`.
- Some newer lostpet responses use Java `record` DTOs with builders, e.g. `aini-inu-backend/src/main/java/scit/ainiinu/lostpet/dto/LostPetResponse.java`.
- Validation includes both field constraints and computed constraints (`@AssertTrue`) as seen in `aini-inu-backend/src/main/java/scit/ainiinu/community/dto/PostCreateRequest.java`.
- DTO style is intentionally mixed (mutable classes + records) across domains rather than fully unified.

## Persistence and Entity Conventions
- JPA auditing is enabled via `aini-inu-backend/src/main/java/scit/ainiinu/common/config/JpaConfig.java` and base timestamps in `aini-inu-backend/src/main/java/scit/ainiinu/common/entity/BaseTimeEntity.java`.
- Entities generally use `@NoArgsConstructor(access = AccessLevel.PROTECTED)` and expose behavior methods instead of public setters.
- Cross-context boundaries are often ID-based instead of direct entity associations, e.g. `authorId`/`memberId` fields in `aini-inu-backend/src/main/java/scit/ainiinu/community/entity/Post.java`.
- Complex reads use custom repository implementations with JPQL and `EntityManager`, e.g. `aini-inu-backend/src/main/java/scit/ainiinu/community/repository/StoryReadRepositoryImpl.java` and `aini-inu-backend/src/main/java/scit/ainiinu/chat/repository/MessageRepositoryImpl.java`.

## Frontend Conventions
- Frontend uses Next.js App Router structure under `aini-inu-frontend/src/app`.
- TypeScript strict mode and path alias `@/*` are configured in `aini-inu-frontend/tsconfig.json`.
- Styling is Tailwind-first with CSS variable theming in `aini-inu-frontend/src/app/globals.css`.
- Common UI atoms are in `aini-inu-frontend/src/components/ui`, e.g. `aini-inu-frontend/src/components/ui/Button.tsx`.
- Utility class composition uses `cn()` in `aini-inu-frontend/src/lib/utils.ts`.
- API access follows service wrappers over shared fetch client: `aini-inu-frontend/src/services/api/apiClient.ts` plus domain services like `aini-inu-frontend/src/services/api/memberService.ts`.
- Local mocked backend behavior in development is standardized through MSW provider and handlers at `aini-inu-frontend/src/mocks/MSWProvider.tsx` and `aini-inu-frontend/src/mocks/handlers.ts`.
- Global client state follows Zustand stores like `aini-inu-frontend/src/store/useUserStore.ts`.

## Tooling and Operational Conventions
- Backend build and dependency management are in `aini-inu-backend/build.gradle` (Java 21, Spring Boot 3.5.x, JUnit platform).
- Environment-driven runtime config is centralized in `aini-inu-backend/src/main/resources/application.properties`.
- OpenAPI export workflow is scripted in `aini-inu-backend/scripts/export-openapi.sh`.
- Docker lifecycle scripts follow a simple `scripts/` convention: `aini-inu-backend/scripts/docker-up.sh`, `aini-inu-backend/scripts/docker-down.sh`, `aini-inu-backend/scripts/docker-logs.sh`.

## Notable Consistency Gaps
- Formatting style is not fully uniform in all Java classes (example spacing/import layout in `aini-inu-backend/src/main/java/scit/ainiinu/pet/entity/Pet.java`).
- DTO modeling style differs by domain (`class` + setters vs `record`), which is workable but increases cognitive switching.
- Frontend still contains generated/base README language and some legacy notes in `aini-inu-frontend/README.md`.
