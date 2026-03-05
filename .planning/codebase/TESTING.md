# Testing Patterns Map

## Test Stack Overview
- Backend tests are JUnit 5 based (`spring-boot-starter-test`) configured in `aini-inu-backend/build.gradle`.
- Mockito is primary for unit and controller-slice collaboration tests (see `given(...)`, `then(...)` patterns).
- Spring test slices are actively used: `@WebMvcTest`, `@DataJpaTest`, and full-context `@SpringBootTest`.
- There is no backend JaCoCo coverage configuration in `aini-inu-backend/build.gradle`.
- Frontend `aini-inu-frontend/package.json` has no `test` script, and no first-party component/unit test suite is present.

## Backend Test Layout
- Test root is `aini-inu-backend/src/test/java/scit/ainiinu`.
- Tests are grouped by domain and by type: `service`, `controller`, `repository`, `integration`, `contract`, `unit`.
- Shared test helper annotation is `aini-inu-backend/src/test/java/scit/ainiinu/testsupport/IntegrationTestProfile.java`.
- Test profile properties are in `aini-inu-backend/src/test/resources/application-test.properties`.

## Unit Test Conventions
- Pure unit tests use `@ExtendWith(MockitoExtension.class)` and `@InjectMocks/@Mock`.
- BDD-style stubbing/verification is common via `org.mockito.BDDMockito`, for example `aini-inu-backend/src/test/java/scit/ainiinu/member/service/AuthServiceTest.java`.
- Tests are often grouped semantically with `@Nested` and Korean `@DisplayName`.
- Service rule tests assert domain exceptions and error codes directly, e.g. `aini-inu-backend/src/test/java/scit/ainiinu/lostpet/unit/LostPetServiceUnitTest.java`.
- Value-object invariant tests are explicit and boundary-driven, e.g. `aini-inu-backend/src/test/java/scit/ainiinu/common/entity/vo/LocationTest.java`.

## Controller Slice Test Conventions
- HTTP contract tests for controllers often use `@WebMvcTest`, MockMvc, and JSON assertions.
- Security boundary is mocked explicitly by replacing auth interceptor + current-member resolver using `@MockitoBean`.
- This pattern appears repeatedly in:
  - `aini-inu-backend/src/test/java/scit/ainiinu/community/controller/PostControllerTest.java`
  - `aini-inu-backend/src/test/java/scit/ainiinu/chat/controller/ChatControllerSliceTest.java`
  - `aini-inu-backend/src/test/java/scit/ainiinu/walk/controller/WalkDiaryControllerContractTest.java`
  - `aini-inu-backend/src/test/java/scit/ainiinu/community/contract/ImagePresignedContractTest.java`
- Many mutating endpoint tests include `with(csrf())`, even though runtime security is interceptor-centric.

## Repository Slice Test Conventions
- Repository behavior tests use `@DataJpaTest` with isolated in-memory DB URLs per class.
- H2 is configured with compatibility modes (`MODE=MySQL` or `MODE=PostgreSQL`) to approximate production SQL behavior.
- Auditing support is imported when needed via `@Import(JpaConfig.class)`, e.g. `aini-inu-backend/src/test/java/scit/ainiinu/walk/repository/WalkDiaryRepositoryTest.java`.
- Lostpet repository slices follow the same pattern in files like `aini-inu-backend/src/test/java/scit/ainiinu/lostpet/repository/LostPetReportRepositorySliceTest.java`.

## Integration Test Conventions
- Full integration tests use `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional`.
- Profile control is standardized with `@IntegrationTestProfile`.
- Representative full-flow tests:
  - `aini-inu-backend/src/test/java/scit/ainiinu/community/integration/ImageUploadIntegrationTest.java`
  - `aini-inu-backend/src/test/java/scit/ainiinu/walk/integration/WalkDiaryCrudIntegrationTest.java`
  - `aini-inu-backend/src/test/java/scit/ainiinu/lostpet/integration/LostPetIntegrationTest.java`
- External integrations are often mocked at bean level inside integration tests (e.g. lostpet AI/chat clients in `LostPetIntegrationTest`).
- WebSocket behavior is covered with real STOMP client integration in `aini-inu-backend/src/test/java/scit/ainiinu/chat/integration/ChatWebSocketIntegrationTest.java`.

## Contract Test Conventions
- OpenAPI/spec contract checks are treated as first-class tests:
  - `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/OpenApiAuthContractTest.java`
  - `aini-inu-backend/src/test/java/scit/ainiinu/common/contract/OpenApiRequestSchemaContractTest.java`
  - `aini-inu-backend/src/test/java/scit/ainiinu/community/contract/StoryOpenApiContractTest.java`
- These tests validate schema constraints, security requirements, and path-level API documentation alignment.
- Endpoint behavior contract tests also exist as controller slices with naming like `*ContractTest` or `*SliceTest`.

## Data, Fixtures, and Test Inputs
- Tests prefer inline fixture construction in each method over shared fixture factories.
- Domain entities are often created through domain factory methods (`create(...)`) and IDs set via reflection when needed.
- JSON request fixtures are frequently written as inline Java text blocks for readability in MockMvc tests.
- Seed SQL exists for runtime/dev data (`aini-inu-backend/src/main/resources/db/seed/*.sql`), but most tests build data directly in code.

## Current Gaps and Risk Areas
- No frontend unit/component/E2E test suite is defined in repo-owned source (`aini-inu-frontend/src` has no `*.test.*`/`*.spec.*` files).
- Frontend behavior confidence currently relies on runtime mocks (`aini-inu-frontend/src/mocks/*`) and manual verification.
- Backend coverage is broad in API and domain logic, but there is no automated coverage gate.
- SQL behavior is partly approximated through H2 compatibility mode; edge differences vs PostgreSQL may still exist.

## How Tests Are Typically Run
- Primary backend command: `./gradlew test` from `aini-inu-backend`.
- Test platform is explicitly JUnit in `tasks.named('test') { useJUnitPlatform() }` within `aini-inu-backend/build.gradle`.
- Some workflows also depend on OpenAPI export/runtime checks via `aini-inu-backend/scripts/export-openapi.sh`.
