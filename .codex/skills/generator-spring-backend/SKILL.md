---
name: generator-spring-backend
description: "Generate production-ready Spring Boot 3.x backend code following DDD principles and team conventions. Use when you need to: (1) Create complete backend layers (Entity/Repository/Service/Controller/DTO) from specifications, (2) Convert DDL to JPA Entities, (3) Generate API endpoints from API specifications, (4) Create domain-specific error codes and exceptions, (5) Build CRUD operations following the team's coding standards (Java 21, constructor injection, Lombok conventions, transaction management, N+1 prevention, soft delete patterns, context isolation via ID references). Supports 아이니이누 project structure with contexts: member, pet, walk, chat, community, lostpet, notification."
---

# Spring Backend Generator

Generate production-ready Spring Boot 3.x backend code for the 아이니이누 (Aini Inu) project following Domain-Driven Design principles and strict team conventions.

## Group / Role Boundary

- Group: `Generator`
- Primary role: 문서 계약 기반 백엔드 코드 생성/수정
- Not in scope: 계약 타당성 검증(검증은 Validator 그룹 담당)
- Handoff:
  - from `validator-api-spec`/`validator-spec-sync`: must-fix 리스트 수신
  - to `generator-spring-test`: 테스트 대상 변경점 전달

## When to Use This Skill

Use this skill when you need to generate backend code for the 아이니이누 project, specifically:

- **Full layer generation**: "Create Member context with all layers"
- **DDL to Entity**: "Convert the member table DDL to a JPA Entity"
- **API to Controller**: "Generate ThreadController from the API spec"
- **CRUD operations**: "Create Pet CRUD with error handling"
- **Context setup**: "Initialize Chat context with base structure"

## External Documentation (Context7 MCP)

**IMPORTANT**: This skill leverages the Context7 MCP server for up-to-date Spring ecosystem documentation.

When generating code or resolving framework-specific questions, use Context7 to query:

- **Spring Boot** (`/spring-projects/spring-boot`) - For configuration, auto-configuration, starters
- **Spring Framework** (`/spring-projects/spring-framework`) - For core concepts, DI, AOP
- **Spring Data JPA** (`/spring-projects/spring-data-jpa`) - For repository patterns, query methods
- **Hibernate** (`/hibernate/hibernate-orm`) - For JPA implementation details, annotations
- **Lombok** (`/projectlombok/lombok`) - For annotation usage and behavior

**Usage Pattern**:
```
1. Use mcp__context7__resolve-library-id to find the library
2. Use mcp__context7__query-docs with specific questions
3. Apply the documentation to code generation
```

**Example**:
- Question: "How to use @EntityGraph to prevent N+1?"
- Action: Query Spring Data JPA docs via Context7
- Apply: Use the latest recommended patterns in generated repository code

This ensures generated code follows the latest best practices and framework conventions.

## Quick Start

### Step 1: Read Canonical Specs

**ALWAYS** start from common docs:

```
view common-docs/PROJECT_PRD.md
view common-docs/API_SPEC.md
view common-docs/FEATURE_SPEC.md
view common-docs/DETAIL_SPEC.md
view common-docs/SDD_SCREEN_STATE_MATRIX.md
```

### Step 2: Read Existing Backend Conventions from Code

Use current code as the convention source:

```
view aini-inu-backend/src/main/java/scit/ainiinu/common/response/ApiResponse.java
view aini-inu-backend/src/main/java/scit/ainiinu/common/exception/BusinessException.java
view aini-inu-backend/src/main/java/scit/ainiinu/member/controller/MemberController.java
```

### Step 3: Read Existing Entity Patterns (if creating entities)

```
view aini-inu-backend/src/main/java/scit/ainiinu/member/entity/Member.java
view aini-inu-backend/src/main/java/scit/ainiinu/pet/entity/Pet.java
```

## Core Principles (Summary)

**MANDATORY - Never Violate**:

1. **No `var`**: Always use explicit types
2. **No setters in Entity**: Use business methods
3. **Constructor injection**: @RequiredArgsConstructor only
4. **Context isolation**: Reference other contexts by ID only
5. **Error handling**: ErrorCode enum per domain + BusinessException
6. **Response format**: ResponseEntity<ApiResponse<T>>
7. **Lazy loading**: All @ManyToOne and @OneToOne
8. **Transaction**: @Transactional(readOnly=true) for queries
9. **Lombok limits**: No @Data in entities, Entity with @NoArgsConstructor(access=PROTECTED), @Setter allowed in DTOs
10. **Dynamic queries**: Use QueryDSL when dynamic query conditions are needed

## Typical Workflow

### Generate Complete Context

```
User: "Create the Pet context with CRUD operations"

Steps:
1. view common-docs/API_SPEC.md
2. view common-docs/DETAIL_SPEC.md
3. view existing context code under `scit/ainiinu/{context}`
4. Create package structure: scit.ainiinu.pet/{entity,repository,service,controller,dto,exception}
5. Generate PetErrorCode enum
6. Generate Pet entity with business methods
7. Generate PetRepository
8. Generate PetService with @Transactional annotations
9. Generate PetController with ResponseEntity<ApiResponse<T>>
10. Generate Request/Response DTOs (class with appropriate Lombok annotations)
```

### Convert DDL to Entity

```
User: "Convert this DDL to a Pet entity"

Steps:
1. view common-docs/DETAIL_SPEC.md
2. view existing entity code (`member/entity`, `pet/entity`)
3. Map SQL types to Java types
4. Add @Entity, @Id, @GeneratedValue
5. Add BaseTimeEntity extends
6. Use @Embeddable for value objects (if applicable)
7. Add business methods (no setters)
8. Add @NoArgsConstructor(access=PROTECTED)
```

### Generate from API Spec

```
User: "Generate ThreadController from the API spec"

Steps:
1. view common-docs/API_SPEC.md
2. view ApiResponse/BusinessException conventions from backend code
3. Parse endpoints, methods, request/response
4. Generate Request DTOs (class with @Getter @Setter @NoArgsConstructor, add jakarta.validation)
5. Generate Response DTOs (class with @Getter @Builder or @AllArgsConstructor)
6. Generate Controller with @RestController, @RequestMapping
7. Use ResponseEntity<ApiResponse<T>> for all endpoints
8. Add @Valid for request validation
```

## Package Structure Reference

```
scit.ainiinu/
├── common/
│   ├── config/          (SecurityConfig, JpaConfig, etc.)
│   ├── security/        (JwtTokenProvider, etc.)
│   ├── response/        (ApiResponse, PageResponse, SliceResponse - 추후 추가 예정)
│   ├── exception/       (GlobalExceptionHandler, BusinessException, ErrorCode interface, CommonErrorCode)
│   ├── entity/          (BaseTimeEntity)
│   └── util/            (DateTimeUtil, LocationUtil)
│
├── member/              (Bounded Context)
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   ├── dto/
│   └── exception/       (MemberErrorCode)
│
├── pet/                 (Bounded Context)
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   ├── dto/
│   └── exception/       (PetErrorCode)
│
└── (other contexts: walk, chat, community, lostpet, notification)
```

## Important Notes

- **Read canonical docs/code FIRST** before generating code
- **Apply policy locks FIRST**: `PROJECT_PRD.md` section 15 and `SDD_SCREEN_STATE_MATRIX.md` override ambiguous behavior
- **Multiple local sources** may be needed for complex tasks
- **Validate against conventions** after generation
- **Test code** should follow the same principles (BDD style, given/when/then)
