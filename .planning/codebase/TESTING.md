# TESTING

## Scope and Priority
- Testing effort is backend-first: `aini-inu-backend` and contract docs are the primary quality gate.
- `common-docs` contract sync is validated via OpenAPI-related tests and snapshot update flow.
- Frontend is pre-refactor; treat current tests as minimal and focus on backend contract safety first.

## Test Suite Topology (Observed)
- Tests are organized by domain under `aini-inu-backend/src/test/java/scit/ainiinu/**`.
- Major styles coexist: unit/service tests, web slice/controller tests, repository slice tests, full integration tests, and contract tests.
- Naming patterns include `*ServiceTest`, `*UnitTest`, `*ControllerTest`, `*SliceTest`, `*ContractTest`, `*IntegrationTest`.

## Unit and Service Tests
- Mockito-based service tests use `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks` (example: `aini-inu-backend/src/test/java/scit/ainiinu/walk/service/WalkDiaryServiceCrudTest.java`).
- Assertions use AssertJ (`assertThat`, `assertThatThrownBy`) and BDD Mockito (`given`, `then`, `willThrow`).
- Test readability is structured with `@Nested` and Korean `@DisplayName` conventions.
- `ReflectionTestUtils` is used when entity IDs must be set in pure unit tests.

## Controller Slice Tests (`@WebMvcTest`)
- API contract behavior is validated with MockMvc in controller slices (examples: `aini-inu-backend/src/test/java/scit/ainiinu/community/controller/PostControllerTest.java`, `walk/controller/WalkDiaryControllerContractTest.java`).
- Security pipeline is stubbed using `@MockitoBean` for `JwtAuthInterceptor` and `CurrentMemberArgumentResolver`.
- Authenticated request simulation commonly uses `@WithMockUser` and `.with(csrf())`.
- Assertions explicitly verify response envelope fields like `$.success`, `$.data.*`, `$.errorCode`.

## Repository Slice Tests (`@DataJpaTest`)
- Repository behavior is isolated with `@DataJpaTest` + `@Import(JpaConfig.class)`.
- H2 in-memory DB is configured in MySQL-compat mode per class via `spring.datasource.url=jdbc:h2:mem:...;MODE=MySQL...`.
- Representative examples:
- `aini-inu-backend/src/test/java/scit/ainiinu/walk/repository/WalkDiaryRepositoryCrudTest.java`
- `aini-inu-backend/src/test/java/scit/ainiinu/chat/repository/ChatRepositorySliceTest.java`
- `aini-inu-backend/src/test/java/scit/ainiinu/lostpet/repository/LostPetReportRepositorySliceTest.java`

## Full Integration Tests (`@SpringBootTest`)
- End-to-end API flows use `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` where needed.
- Shared test profile annotation is `@IntegrationTestProfile` (`aini-inu-backend/src/test/java/scit/ainiinu/testsupport/IntegrationTestProfile.java`).
- Test properties come from `aini-inu-backend/src/test/resources/application-test.properties`.
- JWT-authenticated flow tests generate tokens via `JwtTokenProvider` and assert real permission/error behavior (example: `aini-inu-backend/src/test/java/scit/ainiinu/walk/integration/WalkDiaryCrudIntegrationTest.java`).

## Contract and OpenAPI-Focused Tests
- OpenAPI runtime contract checks exist under `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/**`.
- Security/auth schema alignment is validated in `OpenApiAuthContractTest.java`.
- Request required/min/max schema constraints are validated in `OpenApiRequestSchemaContractTest.java`.
- Domain-specific contract checks also exist, e.g. `community/contract/StoryOpenApiContractTest.java` and `lostpet/contract/LostPetControllerSliceTest.java`.
- This pattern enforces that code-level DTO/annotation changes are reflected in runtime Swagger output.

## Docs and Contract Sync Validation
- OpenAPI snapshot update command: `cd aini-inu-backend && ./scripts/export-openapi.sh`.
- Snapshot target: `common-docs/openapi/openapi.v1.json`.
- Snapshot governance and commit rule are documented in `common-docs/openapi/README.md`.

## Execution Commands
- Backend full tests: `cd aini-inu-backend && ./gradlew test`.
- Backend run for manual verification: `cd aini-inu-backend && ./gradlew bootRun`.
- Frontend lint (limited safety net): `cd aini-inu-frontend && npm run lint`.
- Frontend build smoke check: `cd aini-inu-frontend && npm run build`.

## Frontend Testing Status (Pre-Refactor)
- No dedicated frontend unit/integration test files were found under `aini-inu-frontend/src` using `test/spec/__tests__` naming scans.
- Current frontend quality gates are mostly lint/build and runtime manual checks.
- During refactor readiness work, prioritize adding contract-aware API client tests around `aini-inu-frontend/src/services/api/apiClient.ts` and key domain services before broad component snapshots.

## Practical PR Testing Checklist
- Backend API change: add/update unit + controller slice + integration coverage for the changed path.
- Contract-sensitive change: run OpenAPI contract tests and refresh snapshot.
- Error code or validation change: assert `ApiResponse` envelope (`success/status/errorCode/message`) in controller/integration assertions.
- Persistence change: add repository slice tests for soft-delete filters, unique constraints, and cursor/slice ordering.
- Frontend touch (if unavoidable): run lint/build and validate assumptions against backend OpenAPI contract.
