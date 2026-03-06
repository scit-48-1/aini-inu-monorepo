---
name: spring-test-generator
description: "Generate comprehensive test code for Spring Boot 3.x backend following BDD style and team conventions. Use when you need to: (1) Create unit tests for Service/Repository layers with Mockito, (2) Create integration tests with @SpringBootTest, (3) Create slice tests (@WebMvcTest, @DataJpaTest), (4) Generate test code following given/when/then pattern, (5) Write readable tests with @DisplayName and @Nested. Supports 아이니이누 project structure with contexts: member, pet, walk, chat, community, lostpet, notification."
---

# Spring Test Generator

Generate comprehensive test code for the 아이니이누 (Aini Inu) Spring Boot 3.x project following BDD style and team conventions.

## When to Use This Skill

Use this skill when you need to generate test code for the 아이니이누 project, specifically:

- **Unit tests**: "Create unit tests for PetService"
- **Integration tests**: "Write integration tests for Pet CRUD operations"
- **Slice tests**: "Generate @WebMvcTest for PetController"
- **Repository tests**: "Create @DataJpaTest for PetRepository"
- **Full test suite**: "Generate all tests for Pet context"

## Quick Start

### Step 1: Read Test Conventions

**ALWAYS** start by reading the conventions reference:

```
view references/conventions.md
```

This file contains critical information about:
- Test class naming conventions
- BDD style (given/when/then)
- @DisplayName and @Nested usage
- Test data creation patterns
- Package structure

### Step 2: Choose Test Type

Based on the target layer, read the appropriate pattern file:

| Target | Pattern File | Annotation |
|--------|--------------|------------|
| Service | `references/unit-test-patterns.md` | `@ExtendWith(MockitoExtension.class)` |
| Repository | `references/slice-test-patterns.md` | `@DataJpaTest` |
| Controller | `references/slice-test-patterns.md` | `@WebMvcTest` |
| Full flow | `references/integration-test-patterns.md` | `@SpringBootTest` |

### Step 3: Review Examples

See complete working examples:

```
view references/examples.md
```

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
1. view references/conventions.md
2. view references/unit-test-patterns.md
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
1. view references/conventions.md
2. view references/slice-test-patterns.md
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
1. view references/conventions.md
2. view references/slice-test-patterns.md
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
└── (other contexts: walk, chat, community, lostpet, ...)
```

## Important Notes

- **Read references FIRST** before generating test code
- **Match test structure** to source code structure
- **Use appropriate test type** for each layer
- **Write meaningful @DisplayName** in Korean
- **Group tests with @Nested** for readability

## References

For detailed information, see:
- [Conventions](references/conventions.md) - **READ THIS FIRST**
- [Unit Test Patterns](references/unit-test-patterns.md) - Service layer testing
- [Integration Test Patterns](references/integration-test-patterns.md) - Full flow testing
- [Slice Test Patterns](references/slice-test-patterns.md) - Controller/Repository testing
- [Examples](references/examples.md) - Complete working code samples
