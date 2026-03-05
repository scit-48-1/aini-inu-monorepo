---
name: generator-spring-test
description: "Generate comprehensive test code for Spring Boot 3.x backend following BDD style and team conventions. Use when you need to: (1) Create unit tests for Service/Repository layers with Mockito, (2) Create integration tests with @SpringBootTest, (3) Create slice tests (@WebMvcTest, @DataJpaTest), (4) Generate test code following given/when/then pattern, (5) Write readable tests with @DisplayName and @Nested. Supports 아이니이누 project structure with contexts: member, pet, walk, chat, community, lostpet, notification."
---

# Spring Test Generator

Generate comprehensive test code for the 아이니이누 (Aini Inu) Spring Boot 3.x project following BDD style and team conventions.

## Group / Role Boundary

- Group: `Generator`
- Primary role: 변경된 백엔드 계약/코드를 검증하는 테스트 생성
- Not in scope: API/모델 계약 정의 변경
- Handoff:
  - from `generator-spring-backend`: 변경 파일 및 시나리오 수신
  - to `validator-api-spec`: 테스트 관점에서 발견된 계약 불일치 피드백 전달

## When to Use This Skill

Use this skill when you need to generate test code for the 아이니이누 project, specifically:

- **Unit tests**: "Create unit tests for PetService"
- **Integration tests**: "Write integration tests for Pet CRUD operations"
- **Slice tests**: "Generate @WebMvcTest for PetController"
- **Repository tests**: "Create @DataJpaTest for PetRepository"
- **Full test suite**: "Generate all tests for Pet context"

## Quick Start

### Step 1: Read Test Conventions

**ALWAYS** start by reading canonical docs and current test style:

```
view common-docs/PROJECT_PRD.md
view common-docs/API_SPEC.md
view common-docs/FEATURE_SPEC.md
view common-docs/SDD_SCREEN_STATE_MATRIX.md
view aini-inu-backend/src/test/java
```

This file contains critical information about:
- Test class naming conventions
- BDD style (given/when/then)
- @DisplayName and @Nested usage
- Test data creation patterns
- Package structure

### Step 2: Choose Test Type

Based on the target layer, apply the appropriate test annotation and scope:

| Target | Pattern File | Annotation |
|--------|--------------|------------|
| Service | Local service tests in `src/test/java/**/service` | `@ExtendWith(MockitoExtension.class)` |
| Repository | Local repository tests in `src/test/java/**/repository` | `@DataJpaTest` |
| Controller | Local controller tests in `src/test/java/**/controller` | `@WebMvcTest` |
| Full flow | Existing integration tests | `@SpringBootTest` |

### Step 3: Review Similar Tests in Current Context

Use nearest context tests as baseline and mirror naming/fixture style.

## Core Principles (Summary)

**MANDATORY - Never Violate**:

1. **BDD style**: Use given/when/then pattern in all tests
2. **@DisplayName**: Korean descriptions for test methods
3. **@Nested**: Group related tests by scenario
4. **No `var`**: Always use explicit types
5. **Mockito for unit tests**: @Mock, @InjectMocks, @ExtendWith(MockitoExtension.class)
6. **Slice tests for layers**: @WebMvcTest, @DataJpaTest
7. **Integration tests sparingly**: @SpringBootTest only for full flow tests
8. **Test isolation**: Each test should be independent
9. **Clear assertions**: Use AssertJ for readable assertions

## Test Types Overview

### 1. Unit Tests (Service Layer)

```java
@ExtendWith(MockitoExtension.class)
class PetServiceTest {
    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private PetService petService;

    @Nested
    @DisplayName("반려동물 등록")
    class CreatePet {
        @Test
        @DisplayName("유효한 정보로 반려동물을 등록하면 성공한다")
        void success() {
            // given
            // when
            // then
        }
    }
}
```

### 2. Slice Tests (Controller)

```java
@WebMvcTest(PetController.class)
class PetControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PetService petService;
}
```

### 3. Slice Tests (Repository)

```java
@DataJpaTest
class PetRepositoryTest {
    @Autowired
    private PetRepository petRepository;

    @Autowired
    private TestEntityManager entityManager;
}
```

### 4. Integration Tests

```java
@SpringBootTest
@Transactional
class PetIntegrationTest {
    @Autowired
    private PetService petService;

    @Autowired
    private PetRepository petRepository;
}
```

## Typical Workflow

### Generate Service Unit Tests

```
User: "Create unit tests for PetService"

Steps:
1. view target service and existing `src/test/java/**/service/*Test.java`
2. confirm API/feature constraints from common-docs
3. Read target PetService to understand methods
4. Create PetServiceTest with @ExtendWith(MockitoExtension.class)
5. Add @Mock for dependencies
6. Add @InjectMocks for target service
7. Create @Nested class for each method
8. Write tests following given/when/then pattern
```

### Generate Controller Slice Tests

```
User: "Create tests for PetController"

Steps:
1. view target controller and existing `src/test/java/**/controller/*Test.java`
2. confirm response contract from `common-docs/API_SPEC.md`
3. Read target PetController to understand endpoints
4. Create PetControllerTest with @WebMvcTest(PetController.class)
5. Add @MockBean for service dependencies
6. Write tests using MockMvc
7. Test request validation, response format, error handling
```

### Generate Repository Slice Tests

```
User: "Create tests for PetRepository"

Steps:
1. view target repository and existing `src/test/java/**/repository/*Test.java`
2. confirm data constraints from `common-docs/DETAIL_SPEC.md`
3. Read target PetRepository to understand custom queries
4. Create PetRepositoryTest with @DataJpaTest
5. Use TestEntityManager for test data setup
6. Test custom query methods
7. Test soft delete behavior (if applicable)
```

## Package Structure Reference

```
src/test/java/scit/ainiinu/
├── member/
│   ├── service/
│   │   └── MemberServiceTest.java
│   ├── repository/
│   │   └── MemberRepositoryTest.java
│   └── controller/
│       └── MemberControllerTest.java
│
├── pet/
│   ├── service/
│   │   └── PetServiceTest.java
│   ├── repository/
│   │   └── PetRepositoryTest.java
│   └── controller/
│       └── PetControllerTest.java
│
└── (other contexts: walk, chat, community, lostpet, notification)
```

## Important Notes

- **Read canonical docs/current tests FIRST** before generating test code
- **Honor policy locks** from `PROJECT_PRD.md` section 15 and screen-state rules from `SDD_SCREEN_STATE_MATRIX.md`
- **Match test structure** to source code structure
- **Use appropriate test type** for each layer
- **Write meaningful @DisplayName** in Korean
- **Group tests with @Nested** for readability
