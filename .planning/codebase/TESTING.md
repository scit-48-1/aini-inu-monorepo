# Testing Patterns

**Analysis Date:** 2026-03-06

## Test Framework

**Runner:**
- JUnit 5 (Jupiter) via `spring-boot-starter-test`
- Config: `aini-inu-backend/build.gradle` (line 64: `useJUnitPlatform()`)

**Assertion Library:**
- AssertJ (`org.assertj.core.api.Assertions`)
- Spring MockMvc matchers (`jsonPath`, `status`)

**Mocking Library:**
- Mockito (BDD style via `org.mockito.BDDMockito`)
- `@MockitoBean` (Spring Boot 3.5 replacement for `@MockBean`)

**Run Commands:**
```bash
cd aini-inu-backend
./gradlew test                                    # Run all tests
./gradlew test --tests '*.WalkDiaryServiceTest'   # Run a single test class
./gradlew test --tests '*.walk.*'                  # Run all tests in a package
```

**Frontend Testing:**
- No test runner configured (no Jest, Vitest, or Playwright)
- Validation is via `npm run lint` and `npm run build`

## Test File Organization

**Location:** Mirror package structure under `src/test/java/scit/ainiinu/`

**Naming conventions for test categories:**
- `*Test` -- Unit tests (Mockito-based, no Spring context)
- `*UnitTest` -- Explicit unit test suffix (used in `lostpet/unit/`)
- `*SliceTest` -- Spring slice tests (`@WebMvcTest`, `@DataJpaTest`)
- `*ContractTest` -- Controller contract validation via `@WebMvcTest` or OpenAPI schema checks
- `*IntegrationTest` -- Full `@SpringBootTest` context tests
- `*CoverageTest` -- Additional tests to cover edge cases for coverage

**Directory structure within test packages:**

```
src/test/java/scit/ainiinu/
├── testsupport/
│   └── IntegrationTestProfile.java          # @ActiveProfiles("test") annotation
├── common/
│   ├── contract/                             # OpenAPI contract validation tests
│   │   ├── OpenApiAuthContractTest.java
│   │   ├── OpenApiDocumentationQualityContractTest.java
│   │   ├── OpenApiErrorMatrixSyncContractTest.java
│   │   ├── OpenApiPaginationSortContractTest.java
│   │   ├── OpenApiRequestSchemaContractTest.java
│   │   └── OpenApiResponseStatusContractTest.java
│   ├── entity/vo/LocationTest.java
│   └── security/controller/TestAuthControllerTest.java
├── walk/
│   ├── controller/                           # Slice + contract tests
│   │   ├── WalkDiaryControllerContractTest.java
│   │   ├── WalkDiaryControllerTest.java
│   │   ├── WalkDiaryFollowingControllerContractTest.java
│   │   ├── WalkDiaryThreadLinkControllerContractTest.java
│   │   └── WalkThreadControllerTest.java
│   ├── service/                              # Unit tests
│   │   ├── WalkDiaryServiceTest.java
│   │   ├── WalkDiaryServiceCrudTest.java
│   │   ├── WalkDiaryServiceFollowingTest.java
│   │   ├── WalkDiaryServiceThreadLinkTest.java
│   │   ├── WalkThreadServiceTest.java
│   │   └── WalkThreadServiceCoverageTest.java
│   ├── repository/                           # Data JPA slice tests
│   │   ├── WalkDiaryRepositoryTest.java
│   │   ├── WalkDiaryRepositoryCrudTest.java
│   │   ├── WalkDiaryFollowingRepositoryTest.java
│   │   ├── WalkDiaryThreadLinkRepositoryTest.java
│   │   └── WalkThreadRepositoryTest.java
│   └── integration/                          # Full Spring Boot integration tests
│       ├── WalkDiaryIntegrationTest.java
│       ├── WalkDiaryCrudIntegrationTest.java
│       ├── WalkDiaryFollowingIntegrationTest.java
│       ├── WalkDiaryThreadLinkIntegrationTest.java
│       └── WalkThreadIntegrationTest.java
├── lostpet/
│   ├── unit/                                 # Unit tests
│   ├── contract/                             # Slice tests
│   ├── repository/                           # Data JPA slice tests
│   └── integration/                          # Integration tests
├── chat/
│   ├── controller/                           # Slice tests
│   ├── service/                              # Unit tests
│   ├── repository/                           # Slice tests
│   └── integration/                          # Integration tests
├── community/
│   ├── controller/                           # Controller tests
│   ├── service/                              # Unit tests
│   ├── entity/                               # Entity unit tests
│   ├── unit/                                 # Additional unit tests
│   ├── contract/                             # Contract tests
│   └── integration/                          # Integration tests
├── member/
│   ├── controller/                           # Controller tests
│   └── service/                              # Unit tests
└── pet/
    ├── controller/                           # Controller tests
    └── service/                              # Unit tests
```

## Test Structure

**Suite Organization (unit tests):**
```java
@ExtendWith(MockitoExtension.class)
class WalkDiaryServiceTest {

    @Mock
    private WalkDiaryRepository walkDiaryRepository;

    @Mock
    private WalkThreadRepository walkThreadRepository;

    @InjectMocks
    private WalkDiaryService walkDiaryService;

    @Nested
    @DisplayName("일기 생성")
    class CreateDiary {

        @Test
        @DisplayName("공개 범위 미입력 시 기본값은 PUBLIC(true)다")
        void create_defaultPublic_success() {
            // given
            ...

            // when
            WalkDiaryResponse response = walkDiaryService.createDiary(1L, request);

            // then
            assertThat(response.isPublic()).isTrue();
        }
    }
}
```

**Patterns:**
- `@Nested` classes group tests by operation (create, read, update, delete)
- `@DisplayName` in Korean describes the behavior being tested
- Given/When/Then comment structure in every test
- Method names: `{action}_{condition}_{expectedResult}` (e.g., `create_defaultPublic_success`, `getPrivateDiary_byNonOwner_fail`)

## Test Types

### Unit Tests (`*Test`, `*UnitTest`)

**Scope:** Service layer business logic, entity validation

**Setup:**
```java
@ExtendWith(MockitoExtension.class)
class SomeServiceTest {
    @Mock private SomeRepository someRepository;
    @InjectMocks private SomeService someService;
}
```

**Key patterns:**
- Use `ReflectionTestUtils.setField(entity, "id", 1L)` to set auto-generated IDs on entities
- BDD-style Mockito: `given(repo.findById(1L)).willReturn(Optional.of(entity))`
- Verify interactions: `then(repo).should().save(captor.capture())`
- Exception testing: `assertThatThrownBy(() -> ...).isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", SomeErrorCode.SOME_ERROR)`

### Slice Tests (`*SliceTest`, `*ContractTest` with `@WebMvcTest`)

**Scope:** Controller HTTP contract, repository query correctness

**Controller slice test setup:**
```java
@WebMvcTest(WalkDiaryController.class)
@Import(GlobalExceptionHandler.class)   // optional, to test error responses
class WalkDiaryControllerContractTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private WalkDiaryService walkDiaryService;
    @MockitoBean private JwtAuthInterceptor jwtAuthInterceptor;
    @MockitoBean private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(currentMemberArgumentResolver.supportsParameter(
                argThat(parameter -> parameter.hasParameterAnnotation(CurrentMember.class))
        )).willReturn(true);
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .willReturn(1L);
    }
}
```

**IMPORTANT: Every `@WebMvcTest` must mock these two beans:**
- `JwtAuthInterceptor` (bypass JWT auth)
- `CurrentMemberArgumentResolver` (inject test memberId = 1L)

**Controller test assertions:**
```java
mockMvc.perform(post("/api/v1/walk-diaries")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.success").value(true))
    .andExpect(jsonPath("$.data.id").value(11L));
```

**Note:** `@WithMockUser` is required on each test method for Spring Security.

**Repository slice test setup:**
```java
@DataJpaTest(properties = "spring.datasource.url=jdbc:h2:mem:test-name;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Import(JpaConfig.class)
class WalkDiaryRepositoryTest {
    @Autowired private WalkDiaryRepository walkDiaryRepository;
}
```

**Each `@DataJpaTest` specifies a unique H2 database URL** to prevent collisions between test classes.

### Integration Tests (`*IntegrationTest`)

**Scope:** Full request lifecycle from HTTP to database

**Setup:**
```java
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:walkdiary-int;MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
@IntegrationTestProfile
class WalkDiaryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MemberRepository memberRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;
}
```

**Key patterns:**
- `@IntegrationTestProfile` activates test profile (H2, no AI auto-config)
- `@Transactional` rolls back after each test
- Create real entities (Member, etc.) and generate real JWT tokens via `JwtTokenProvider`
- Assert full API contract including nested JSON paths

**Integration test flow example:**
```java
@Test
void createPatchListDiary_success() throws Exception {
    // 1. Create a real member
    Member member = memberRepository.save(Member.builder()
            .email("test@test.com")
            .nickname("tester")
            .memberType(MemberType.PET_OWNER)
            .build());
    String token = jwtTokenProvider.generateAccessToken(member.getId());

    // 2. POST to create
    mockMvc.perform(post("/api/v1/walk-diaries")
            .with(csrf())
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // 3. PATCH to update
    // 4. GET to verify list
}
```

### OpenAPI Contract Tests

**Location:** `src/test/java/scit/ainiinu/common/contract/`

**Purpose:** Validate that the live OpenAPI spec (generated by springdoc) matches expected contracts

**Tests include:**
- `OpenApiResponseStatusContractTest` -- Validates response codes match a registry
- `OpenApiAuthContractTest` -- Validates security requirements
- `OpenApiDocumentationQualityContractTest` -- Validates schema documentation quality
- `OpenApiPaginationSortContractTest` -- Validates pagination parameter specs
- `OpenApiRequestSchemaContractTest` -- Validates request body schemas
- `OpenApiErrorMatrixSyncContractTest` -- Validates error code matrix alignment

**These are `@SpringBootTest` + `@IntegrationTestProfile` tests** that fetch `/v3/api-docs` and parse the JSON.

## Mocking

**Framework:** Mockito (via `spring-boot-starter-test`)

**BDD-style API used throughout:**
```java
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

// Stubbing
given(repository.findById(1L)).willReturn(Optional.of(entity));
given(repository.save(any(Entity.class))).willAnswer(invocation -> {
    Entity e = invocation.getArgument(0);
    ReflectionTestUtils.setField(e, "id", 1L);
    return e;
});

// Verification
then(repository).should().save(captor.capture());
then(repository).shouldHaveNoMoreInteractions();
```

**What to Mock:**
- Repositories in service unit tests
- Services in controller slice tests
- `JwtAuthInterceptor` and `CurrentMemberArgumentResolver` in all `@WebMvcTest` tests
- External service clients (chat integration, AI client)

**What NOT to Mock:**
- Entities (use real factory methods: `WalkDiary.create(...)`)
- DTOs (construct with setters/builders)
- In integration tests: nothing is mocked (full stack)

**Setting Entity IDs in Tests:**
```java
// Entities use IDENTITY generation, so IDs are null until persisted.
// In unit tests, use ReflectionTestUtils:
ReflectionTestUtils.setField(entity, "id", 1L);

// Some entities provide a test helper method:
report.assignIdForTest(1L);
```

## Fixtures and Factories

**Test Data:**
- No shared fixture files or factory classes
- Each test creates its own test data inline using entity factory methods and DTO setters/builders

**Entity creation pattern in tests:**
```java
// Create entity via static factory
WalkDiary diary = WalkDiary.create(1L, null, "Title", "Content", List.of(), LocalDate.now(), true);
ReflectionTestUtils.setField(diary, "id", 11L);

// Create request DTO
WalkDiaryCreateRequest request = new WalkDiaryCreateRequest();
request.setTitle("Title");
request.setContent("Content");
request.setWalkDate(LocalDate.now());

// Create response DTO for stubbing
WalkDiaryResponse response = WalkDiaryResponse.builder()
    .id(11L)
    .memberId(1L)
    .title("Title")
    .build();
```

**Integration test member creation:**
```java
Member member = memberRepository.save(Member.builder()
    .email("test@test.com")
    .nickname("tester")
    .memberType(MemberType.PET_OWNER)
    .build());
String token = jwtTokenProvider.generateAccessToken(member.getId());
```

## Test Configuration

**Test profile:** `src/test/resources/application-test.properties`

**Key test properties:**
```properties
# H2 in-memory with PostgreSQL mode
spring.datasource.url=jdbc:h2:mem:ainidb-test;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# Disable SQL init scripts
spring.sql.init.mode=never

# Disable AI/vector store auto-configuration
spring.autoconfigure.exclude=org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration
spring.ai.model.chat=none
spring.ai.model.embedding.text=none
```

**Additional properties on tests:**
- Each `@DataJpaTest` and `@SpringBootTest` specifies its own unique H2 URL to avoid database collisions
- The `MODE=MySQL` or `MODE=PostgreSQL` varies between tests (inconsistency noted)

**Test support annotation:**
```java
// Located at: src/test/java/scit/ainiinu/testsupport/IntegrationTestProfile.java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
public @interface IntegrationTestProfile {}
```

## Coverage

**Requirements:** Not enforced (no JaCoCo or coverage plugin configured in `build.gradle`)

**View Coverage:**
```bash
# IDE-based coverage only (IntelliJ IDEA run with coverage)
# No CLI coverage command available
```

## Common Patterns

**Async Testing:**
- Not applicable; all backend tests are synchronous MockMvc-based
- WebSocket integration test exists at `chat/integration/ChatWebSocketIntegrationTest.java`

**Error Testing:**
```java
// Assert domain exception with specific error code
assertThatThrownBy(() -> walkDiaryService.getDiary(1L, 1L))
    .isInstanceOf(BusinessException.class)
    .hasFieldOrPropertyWithValue("errorCode", WalkDiaryErrorCode.DIARY_PRIVATE);

// Assert HTTP error via MockMvc
mockMvc.perform(post("/api/v1/lost-pets/analyze")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(request))
    .andExpect(status().isInternalServerError())
    .andExpect(jsonPath("$.errorCode").value("L500_AI_ANALYZE_FAILED"));

// Assert validation error
mockMvc.perform(post("/api/v1/lost-pets/1/match")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidRequest))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.errorCode").value("C002"));
```

**Pagination Testing:**
```java
// Create Slice test data
PageRequest pageable = PageRequest.of(0, 20);
Slice<Entity> slice = new SliceImpl<>(List.of(entity), pageable, false);
given(repository.findBySomething(any(), any())).willReturn(slice);

// Assert SliceResponse structure
mockMvc.perform(get("/api/v1/resource").param("page", "0").param("size", "20"))
    .andExpect(jsonPath("$.data.content[0].id").value(11L))
    .andExpect(jsonPath("$.data.hasNext").value(false));
```

## Writing New Tests

**For a new service unit test:**
1. Create `{Domain}ServiceTest.java` in `src/test/java/scit/ainiinu/{domain}/service/`
2. Use `@ExtendWith(MockitoExtension.class)`
3. Mock repositories with `@Mock`, inject service with `@InjectMocks`
4. Group by operation with `@Nested` + `@DisplayName`
5. Follow given/when/then pattern

**For a new controller contract test:**
1. Create `{Controller}ContractTest.java` in `src/test/java/scit/ainiinu/{domain}/controller/`
2. Use `@WebMvcTest({Controller}.class)`
3. Mock `JwtAuthInterceptor` + `CurrentMemberArgumentResolver` in `@BeforeEach`
4. Mock service with `@MockitoBean`
5. Add `@WithMockUser` on each test method
6. Assert both `$.success` and `$.data.*` fields

**For a new integration test:**
1. Create `{Feature}IntegrationTest.java` in `src/test/java/scit/ainiinu/{domain}/integration/`
2. Use `@SpringBootTest` with unique H2 URL
3. Add `@AutoConfigureMockMvc`, `@Transactional`, `@IntegrationTestProfile`
4. Create real entities, generate JWT tokens, exercise full API flow

---

*Testing analysis: 2026-03-06*
