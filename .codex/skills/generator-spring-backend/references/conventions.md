# Team Conventions & Coding Standards

## Package Structure

### Project Structure
```
com.ainiinu/
├── common/                  (Global shared code)
│   ├── config/
│   ├── security/
│   ├── response/
│   ├── exception/
│   ├── entity/
│   └── util/
│
├── member/                  (Bounded Context)
│   ├── entity/
│   ├── repository/
│   ├── service/
│   ├── controller/
│   ├── dto/
│   └── exception/
│
├── pet/                     (Bounded Context)
├── walk/                    (Bounded Context)
├── chat/                    (Bounded Context)
├── community/               (Bounded Context)
├── lostpet/                 (Bounded Context)
└── notification/            (Bounded Context)
```

### Layer Responsibilities

| Layer | Purpose | Annotations |
|-------|---------|-------------|
| `controller/` | HTTP endpoints, request/response handling | @RestController, @RequestMapping |
| `service/` | Business logic, transactions | @Service, @Transactional |
| `repository/` | Data access | extends JpaRepository |
| `entity/` | Domain models, business rules | @Entity, @Embeddable |
| `dto/` | API contracts (request/response) | record or class |
| `exception/` | Domain-specific error codes | enum implements ErrorCode |

## DDD Principles

### 1. Context Isolation (ID-Only References)

**CRITICAL**: Different bounded contexts MUST reference each other by ID only.

```java
// ❌ WRONG: Direct entity reference across contexts
@Entity
public class Thread {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Member author;  // Member is in different context
}

// ✅ CORRECT: ID-only reference
@Entity
public class Thread {
    private Long authorId;  // Only store Member's ID
}
```

**Why?**
- Maintains bounded context independence
- Prevents coupling between contexts
- Clear transaction boundaries
- Avoids N+1 issues across contexts

**When you need data from another context:**
```java
@Service
@RequiredArgsConstructor
public class ThreadService {
    private final ThreadRepository threadRepository;
    private final MemberRepository memberRepository;  // OK to inject

    public ThreadDetailResponse getThreadDetail(Long threadId) {
        Thread thread = threadRepository.findById(threadId)...;
        Member author = memberRepository.findById(thread.getAuthorId())...;
        return ThreadDetailResponse.of(thread, author);
    }
}
```

### 2. Value Objects (@Embeddable)

Use value objects to encapsulate related fields:

```java
// ❌ WRONG: Primitive obsession
@Entity
public class Thread {
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

// ✅ CORRECT: Value object
@Embeddable
public class Location {
    private String placeName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    public double distanceTo(Location other) {
        // Distance calculation logic
    }
}

@Entity
public class Thread {
    @Embedded
    private Location location;
}
```

**Common value objects in 아이니이누:**
- Location (placeName, latitude, longitude)
- MannerTemperature (value with business logic)
- TimeRange (startTime, endTime)

### 3. Domain Logic in Entities

Business rules belong in entities, NOT in services.

```java
// ❌ WRONG: Business logic in service
@Service
public class MemberService {
    public void addMannerScore(Member member, int score) {
        if (score < 1 || score > 10) {
            throw new IllegalArgumentException("Invalid score");
        }
        member.setMannerTemperature(...); // Direct setter
    }
}

// ✅ CORRECT: Business logic in entity
@Entity
public class Member {
    private BigDecimal mannerTemperature;
    private int mannerScoreSum;
    private int mannerScoreCount;
    
    public void addMannerScore(int score) {
        validateScore(score);
        this.mannerScoreSum += score;
        this.mannerScoreCount += 1;
        this.mannerTemperature = calculateAverage();
    }
    
    private void validateScore(int score) {
        if (score < 1 || score > 10) {
            throw new BusinessException(MemberErrorCode.INVALID_MANNER_SCORE);
        }
    }
    
    private BigDecimal calculateAverage() {
        // Calculation logic
    }
}
```

## Java & Spring Conventions

### 1. Type Declarations

**MANDATORY**: No `var` keyword. Always use explicit types.

```java
// ❌ WRONG
var member = memberRepository.findById(id);
var list = new ArrayList<String>();

// ✅ CORRECT
Member member = memberRepository.findById(id)...;
List<String> list = new ArrayList<>();
```

### 2. Lombok Usage

**Entities (STRICT):**
- ✅ `@Getter` - Allowed
- ✅ `@ToString` - Allowed
- ✅ `@Builder` - Allowed
- ✅ `@NoArgsConstructor(access = PROTECTED)` - MANDATORY
- ❌ `@Setter` - FORBIDDEN (use business methods instead)
- ❌ `@Data` - FORBIDDEN (includes @Setter)

**Services:**
- ✅ `@RequiredArgsConstructor` - MANDATORY (constructor injection)
- ✅ `@Slf4j` - Recommended for logging

**DTOs (FLEXIBLE):**
- ✅ `@Getter` - Always use
- ✅ `@Setter` - ALLOWED (especially for Request DTOs)
- ✅ `@NoArgsConstructor` - ALLOWED (needed by Jackson)
- ✅ `@AllArgsConstructor` - ALLOWED (convenience)
- ✅ `@Builder` - ALLOWED (for complex Response DTOs)
- ⚠️ `@Data` - ALLOWED but not recommended (prefer explicit annotations)

**Examples:**
```java
// ✅ Entity
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Member {
    private String name;
    
    // No @Setter! Use business methods
    public void updateName(String name) {
        this.name = name;
    }
}

// ✅ Service
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
}

// ✅ Request DTO
@Getter
@Setter
@NoArgsConstructor
public class PetRegisterRequest {
    @NotBlank
    private String name;
    
    @NotNull
    private Integer age;
}

// ✅ Response DTO (Option 1: Builder)
@Getter
@Builder
public class PetResponse {
    private Long id;
    private String name;
    private Integer age;
    
    public static PetResponse from(Pet pet) {
        return PetResponse.builder()
            .id(pet.getId())
            .name(pet.getName())
            .age(pet.getAge())
            .build();
    }
}

// ✅ Response DTO (Option 2: AllArgsConstructor)
@Getter
@AllArgsConstructor
public class PetResponse {
    private Long id;
    private String name;
    private Integer age;
    
    public static PetResponse from(Pet pet) {
        return new PetResponse(
            pet.getId(),
            pet.getName(),
            pet.getAge()
        );
    }
}
```

### 3. Dependency Injection

**MANDATORY**: Constructor injection only (via @RequiredArgsConstructor)

```java
// ❌ WRONG: Field injection
@Service
public class MemberService {
    @Autowired
    private MemberRepository memberRepository;
}

// ✅ CORRECT: Constructor injection
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
}
```

## JPA Conventions

### 1. Lazy Loading

**MANDATORY**: All @ManyToOne and @OneToOne MUST be LAZY.

```java
// ❌ WRONG: Default EAGER loading
@ManyToOne
@JoinColumn(name = "breed_id")
private Breed breed;

// ✅ CORRECT: Explicit LAZY
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "breed_id")
private Breed breed;
```

### 2. N+1 Problem Prevention

Be conscious of N+1 issues. Use fetch joins or @EntityGraph when loading collections.

```java
// ❌ WRONG: Will cause N+1
List<Pet> pets = petRepository.findByMemberId(memberId);
// Later: pets.forEach(pet -> pet.getBreed().getName()); // N+1!

// ✅ CORRECT: Fetch join
@Query("SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.memberId = :memberId")
List<Pet> findByMemberIdWithBreed(@Param("memberId") Long memberId);
```

### 3. Dynamic Queries with QueryDSL

**MANDATORY**: Use QueryDSL for dynamic query conditions (filtering, conditional sorting, complex searches).

**When to use QueryDSL:**
- Multiple optional search filters (e.g., search by name, breed, age range)
- Conditional sorting based on user input
- Complex WHERE clause combinations
- Type-safe query construction

**Repository Pattern:**
```java
// 1. Create custom repository interface
public interface ThreadRepositoryCustom {
    List<Thread> searchThreads(ThreadSearchCondition condition);
}

// 2. Implement with QueryDSL
@RequiredArgsConstructor
public class ThreadRepositoryImpl implements ThreadRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Thread> searchThreads(ThreadSearchCondition condition) {
        return queryFactory
            .selectFrom(thread)
            .where(
                titleContains(condition.getTitle()),
                locationEq(condition.getLocation()),
                walkDateBetween(condition.getStartDate(), condition.getEndDate())
            )
            .orderBy(orderByCondition(condition.getSort()))
            .fetch();
    }

    private BooleanExpression titleContains(String title) {
        return hasText(title) ? thread.title.contains(title) : null;
    }

    private BooleanExpression locationEq(String location) {
        return hasText(location) ? thread.location.placeName.eq(location) : null;
    }

    private BooleanExpression walkDateBetween(LocalDate start, LocalDate end) {
        if (start == null && end == null) return null;
        if (start == null) return thread.walkDate.loe(end);
        if (end == null) return thread.walkDate.goe(start);
        return thread.walkDate.between(start, end);
    }

    private OrderSpecifier<?> orderByCondition(String sort) {
        if (sort == null) return thread.createdAt.desc();
        return switch (sort) {
            case "date" -> thread.walkDate.asc();
            case "popular" -> thread.viewCount.desc();
            default -> thread.createdAt.desc();
        };
    }
}

// 3. Main repository extends both
public interface ThreadRepository extends JpaRepository<Thread, Long>, ThreadRepositoryCustom {
    // Standard methods
}
```

**Service Usage:**
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThreadService {
    private final ThreadRepository threadRepository;

    public List<ThreadResponse> searchThreads(ThreadSearchCondition condition) {
        List<Thread> threads = threadRepository.searchThreads(condition);
        return threads.stream()
            .map(ThreadResponse::from)
            .toList();
    }
}
```

**Why QueryDSL?**
- Type-safe queries (compile-time error checking)
- Cleaner than Criteria API
- Dynamic query composition with null-safe BooleanExpression
- Better IDE support with Q-types

**Configuration Required:**
```xml
<!-- build.gradle -->
dependencies {
    implementation 'com.querydsl:querydsl-jpa:5.0.0:jakarta'
    annotationProcessor "com.querydsl:querydsl-apt:5.0.0:jakarta"
    annotationProcessor "jakarta.annotation:jakarta.annotation-api"
    annotationProcessor "jakarta.persistence:jakarta.persistence-api"
}
```

**JPAQueryFactory Bean:**
```java
@Configuration
public class QueryDslConfig {
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
```

### 3. Entity Updates (No Setters, Use Dirty Checking)

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {
    private String name;
    private int age;
    
    // ❌ No setters!
    
    // ✅ Business method
    public void updateInfo(String name, int age) {
        validateName(name);
        validateAge(age);
        this.name = name;
        this.age = age;
    }
}

@Service
@RequiredArgsConstructor
public class PetService {
    @Transactional  // NOT readOnly
    public PetResponse updatePet(Long petId, PetUpdateRequest request) {
        Pet pet = petRepository.findById(petId)...;
        pet.updateInfo(request.name(), request.age());
        // NO repository.save() needed! Dirty checking handles it
        return PetResponse.from(pet);
    }
}
```

### 4. Base Entity

All entities MUST extend BaseTimeEntity:

```java
@MappedSuperclass
@Getter
public abstract class BaseTimeEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

## Transaction Management

### Service Layer Rules

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Default for entire class
public class MemberService {

    // ✅ Read-only method (inherits @Transactional(readOnly=true))
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)...;
        return MemberResponse.from(member);
    }
    
    // ✅ Write method (override with @Transactional)
    @Transactional
    public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)...;
        member.updateProfile(request.nickname(), request.profileImageUrl());
        return MemberResponse.from(member);
    }
}
```

**Why readOnly = true?**
- Performance optimization
- Prevents accidental data modifications
- Clear intent: this method only reads

## DTO Conventions

### Use `class` for DTOs

All DTOs should use standard Java classes, not records (for team familiarity).

**Request DTO Pattern:**
```java
// ✅ Request DTO with validation
@Getter
@Setter
@NoArgsConstructor
public class PetRegisterRequest {
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 10, message = "이름은 최대 10자입니다")
    private String name;
    
    @NotNull(message = "나이는 필수입니다")
    @Min(value = 0, message = "나이는 0 이상이어야 합니다")
    private Integer age;
    
    @NotNull(message = "견종은 필수입니다")
    private Long breedId;
}
```

**Response DTO Pattern (Option 1: Builder - Recommended for complex DTOs):**
```java
// ✅ Response DTO with Builder
@Getter
@Builder
public class PetResponse {
    private Long id;
    private String name;
    private Integer age;
    private String breedName;
    private Boolean isMain;
    
    public static PetResponse from(Pet pet) {
        return PetResponse.builder()
            .id(pet.getId())
            .name(pet.getName())
            .age(pet.getAge())
            .breedName(pet.getBreed().getName())
            .isMain(pet.getIsMain())
            .build();
    }
}
```

**Response DTO Pattern (Option 2: AllArgsConstructor - For simple DTOs):**
```java
// ✅ Response DTO with AllArgsConstructor
@Getter
@AllArgsConstructor
public class PetSimpleResponse {
    private Long id;
    private String name;
    
    public static PetSimpleResponse from(Pet pet) {
        return new PetSimpleResponse(pet.getId(), pet.getName());
    }
}
```

**Why @Setter in Request DTOs?**
- Jackson (JSON deserializer) needs setters to populate fields
- Alternative: use @JsonProperty on fields, but @Setter is simpler
- Request DTOs are mutable by nature (incoming data)

**Why no @Setter in Response DTOs?**
- Response DTOs should be immutable
- Use @Builder or constructor for creation
- Never modified after construction

## Validation

Use jakarta.validation in DTOs:

```java
public record ThreadCreateRequest(
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 30, message = "제목은 최대 30자입니다")
    String title,
    
    @NotBlank(message = "소개글은 필수입니다")
    @Size(max = 500, message = "소개글은 최대 500자입니다")
    String description,
    
    @NotNull(message = "산책 날짜는 필수입니다")
    @FutureOrPresent(message = "과거 날짜는 선택할 수 없습니다")
    LocalDate walkDate
) {}
```

Controller:
```java
@PostMapping
public ResponseEntity<ApiResponse<ThreadResponse>> createThread(
    @Valid @RequestBody ThreadCreateRequest request  // @Valid triggers validation
) {
    // ...
}
```

## Logging

Use @Slf4j:

```java
@Slf4j
@Service
public class MemberService {
    public void someMethod() {
        log.info("Member {} logged in", memberId);
        log.warn("Failed login attempt for email: {}", email);
        log.error("Unexpected error processing member {}", memberId, exception);
        log.debug("Debug info: {}", data);
    }
}
```

**NEVER use System.out.println**

## Context Communication

Use events for cross-context communication:

```java
// ❌ WRONG: Direct service call across contexts
@Service
public class ThreadService {
    private final NotificationService notificationService;  // Different context

    public void createThread(...) {
        Thread thread = save(...);
        notificationService.sendNotification(...);  // Tight coupling
    }
}

// ✅ CORRECT: Event-based communication
@Service
@RequiredArgsConstructor
public class ThreadService {
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public void createThread(...) {
        Thread thread = save(...);
        eventPublisher.publishEvent(new ThreadCreatedEvent(thread.getId()));
    }
}

// In notification context
@Component
public class NotificationEventListener {
    @EventListener
    @Async
    public void handleThreadCreated(ThreadCreatedEvent event) {
        // Send notification
    }
}
```

## Testing Conventions

Use BDD style (given/when/then):

```java
@DisplayName("회원 프로필 업데이트")
@Test
void updateProfile_shouldUpdateNickname_whenValid() {
    // given
    Member member = Member.builder()
            .email("test@example.com")
            .nickname("oldNick")
            .build();
    memberRepository.save(member);

    MemberUpdateRequest request = new MemberUpdateRequest("newNick", null);

    // when
    MemberResponse response = memberService.updateProfile(member.getId(), request);

    // then
    assertThat(response.nickname()).isEqualTo("newNick");

    Member updated = memberRepository.findById(member.getId()).orElseThrow();
    assertThat(updated.getNickname()).isEqualTo("newNick");
}
```

## Naming Conventions

- **Classes**: PascalCase (MemberService, PetRepository)
- **Methods**: camelCase (getMember, updateProfile)
- **Variables**: camelCase (memberId, petList)
- **Constants**: UPPER_SNAKE_CASE (MAX_PET_COUNT)
- **Database**: lower_snake_case (member_id, created_at)
- **Packages**: lowercase (member, pet, walk)

## Critical Reminders

1. **No var** - Always explicit types
2. **No @Setter in entities** - Use business methods
3. **No @Data** - Use specific Lombok annotations
4. **@NoArgsConstructor(access=PROTECTED)** for entities
5. **Context isolation** - ID references only
6. **Lazy loading** - Always for @ManyToOne/@OneToOne
7. **@Transactional(readOnly=true)** for queries
8. **Dirty checking** - No save() for updates
9. **Value objects** - Use @Embeddable
10. **Events** - For cross-context communication