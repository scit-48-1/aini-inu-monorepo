# JPA Entity Patterns

## Entity Structure Template

```java
package com.ainiinu.{context}.entity;

import com.ainiinu.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityName extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Fields (primitives, references)

    // Value objects (@Embedded)

    // Builder (optional, for complex initialization)
    @Builder
    private EntityName(/* parameters */) {
        // Initialize and validate
    }

    // Business methods (NO SETTERS)

    // Private helper methods
}
```

## Core Principles

### 1. No Setters - Business Methods Only

```java
// ❌ WRONG: Public setters
@Entity
public class Pet {
    private String name;
    private int age;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
}

// ✅ CORRECT: Business methods with validation
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseTimeEntity {
    private String name;
    private int age;
    
    @Builder
    private Pet(String name, int age) {
        validateName(name);
        validateAge(age);
        this.name = name;
        this.age = age;
    }
    
    public void updateInfo(String name, int age) {
        if (name != null) {
            validateName(name);
            this.name = name;
        }
        if (age >= 0) {
            validateAge(age);
            this.age = age;
        }
    }
    
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(PetErrorCode.INVALID_PET_NAME);
        }
        if (name.length() > 10) {
            throw new BusinessException(PetErrorCode.PET_NAME_TOO_LONG);
        }
    }
    
    private void validateAge(int age) {
        if (age < 0 || age > 30) {
            throw new BusinessException(PetErrorCode.INVALID_PET_AGE);
        }
    }
}
```

### 2. Context Isolation - ID-Only References

```java
// ❌ WRONG: Direct entity reference across contexts
@Entity
public class Pet {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;  // Member is in different context
}

// ✅ CORRECT: Store only ID
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseTimeEntity {
    @Column(nullable = false)
    private Long memberId;  // Reference by ID only
    
    // No Member object here!
}
```

**Exception**: Within the same context, relationships are OK:

```java
// ✅ OK: Same context (Pet context)
@Entity
public class Pet {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", nullable = false)
    private Breed breed;  // Breed is in Pet context - OK!
}
```

### 3. Lazy Loading (MANDATORY)

```java
// ❌ WRONG: Default EAGER
@ManyToOne
@JoinColumn(name = "breed_id")
private Breed breed;

// ✅ CORRECT: Explicit LAZY
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "breed_id", nullable = false)
private Breed breed;
```

**ALL @ManyToOne and @OneToOne MUST be LAZY**

### 4. BaseTimeEntity (MANDATORY)

All entities MUST extend BaseTimeEntity:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
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

Enable JPA Auditing in config:

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

## DDL to Entity Mapping

### Type Mapping

| SQL Type | Java Type | Notes |
|----------|-----------|-------|
| BIGINT | Long | Use for IDs |
| INT | Integer | Use for counts, ages |
| VARCHAR | String | |
| TEXT | String | |
| DECIMAL(p,s) | BigDecimal | For precise numbers (money, scores) |
| DATE | LocalDate | |
| DATETIME | LocalDateTime | |
| TINYINT(1) | Boolean | MySQL boolean |

### Column Attributes

| SQL | JPA |
|-----|-----|
| `PRIMARY KEY` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` |
| `NOT NULL` | `@Column(nullable = false)` |
| `UNIQUE` | `@Column(unique = true)` |
| `VARCHAR(50)` | `@Column(length = 50)` |
| `DEFAULT` | Set in constructor/builder |
| `COMMENT` | `@Column(columnDefinition = "...")` (optional) |

### Enum Mapping

```sql
status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, BANNED'
```

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private MemberStatus status = MemberStatus.ACTIVE;

public enum MemberStatus {
    ACTIVE, INACTIVE, BANNED
}
```

**ALWAYS use EnumType.STRING** (never ORDINAL)

## Repository Patterns

### Method Naming Convention (Preferred)

Spring Data JPA can generate queries from method names automatically. Use this for simple queries.

**Common Keywords:**

| Keyword | Purpose | Example |
|---------|---------|---------|
| `findBy` | Single or multiple results | `findByName(String name)` |
| `findAllBy` | Multiple results (explicit) | `findAllByAge(int age)` |
| `countBy` | Count records | `countByMemberId(Long memberId)` |
| `existsBy` | Check existence | `existsByEmail(String email)` |
| `deleteBy` | Delete records | `deleteByIdAndMemberId(Long id, Long memberId)` |
| `And` | Logical AND | `findByNameAndAge(String name, int age)` |
| `Or` | Logical OR | `findByNameOrEmail(String name, String email)` |
| `IsNull` | NULL check | `findByDeletedAtIsNull()` |
| `IsNotNull` | NOT NULL check | `findByDeletedAtIsNotNull()` |
| `True` | Boolean true | `findByIsMainTrue()` |
| `False` | Boolean false | `findByIsMainFalse()` |

**Examples:**

```java
public interface PetRepository extends JpaRepository<Pet, Long> {
    
    // ✅ Simple queries - method naming is enough
    List<Pet> findByMemberId(Long memberId);
    
    long countByMemberId(Long memberId);
    
    Optional<Pet> findByMemberIdAndIsMainTrue(Long memberId);
    
    boolean existsByIdAndMemberId(Long id, Long memberId);
    
    List<Pet> findByAgeGreaterThan(int age);
}
```

### When to Use @Query

Only use `@Query` when method naming is insufficient.

**✅ Use @Query for:**

**1. Fetch Joins (N+1 Prevention)**
```java
// Without Fetch Join: N+1 problem (1 query for pets + N queries for breeds)
List<Pet> findByMemberId(Long memberId);
// Later: pets.forEach(pet -> pet.getBreed().getName()); // N+1!

// ✅ With Fetch Join: Single query
@Query("SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.memberId = :memberId")
List<Pet> findByMemberIdWithBreed(@Param("memberId") Long memberId);
```

**2. Complex Joins**
```java
@Query("SELECT t FROM Thread t " +
       "JOIN FETCH t.threadPets tp " +
       "JOIN FETCH tp.pet " +
       "WHERE t.id = :id")
Optional<Thread> findByIdWithPets(@Param("id") Long id);
```

**3. Aggregation Queries**
```java
@Query("SELECT AVG(m.mannerTemperature) FROM Member m WHERE m.status = 'ACTIVE'")
BigDecimal getAverageMannerTemperature();

@Query("SELECT NEW com.ainiinu.pet.dto.BreedStatistics(p.breed.name, COUNT(p)) " +
       "FROM Pet p WHERE p.deletedAt IS NULL GROUP BY p.breed")
List<BreedStatistics> getBreedStatistics();
```

**4. Native Queries (when JPQL is insufficient)**
```java
@Query(value = "SELECT * FROM pet WHERE ST_Distance_Sphere(" +
               "point(longitude, latitude), point(:lon, :lat)) <= :radius",
       nativeQuery = true)
List<Pet> findNearby(@Param("lat") double lat, 
                     @Param("lon") double lon, 
                     @Param("radius") double radius);
```

**❌ Don't use @Query for:**

**1. Simple Conditions**
```java
// ❌ Unnecessary
@Query("SELECT p FROM Pet p WHERE p.memberId = :memberId")
List<Pet> findByMemberId(@Param("memberId") Long memberId);

// ✅ Method naming is enough
List<Pet> findByMemberId(Long memberId);
```

**2. Simple Counting**
```java
// ❌ Unnecessary
@Query("SELECT COUNT(p) FROM Pet p WHERE p.memberId = :memberId AND p.deletedAt IS NULL")
long countPets(@Param("memberId") Long memberId);

// ✅ Method naming is enough
long countByMemberIdAndDeletedAtIsNull(Long memberId);
```

**3. Boolean Conditions**
```java
// ❌ Unnecessary
@Query("SELECT p FROM Pet p WHERE p.memberId = :memberId AND p.isMain = true")
Optional<Pet> findMainPet(@Param("memberId") Long memberId);

// ✅ Method naming is enough
Optional<Pet> findByMemberIdAndIsMainTrue(Long memberId);
```

### Repository Best Practices

```java
public interface PetRepository extends JpaRepository<Pet, Long> {
    
    // ✅ GOOD: Method naming for simple queries
    List<Pet> findByMemberId(Long memberId);
    long countByMemberId(Long memberId);
    boolean existsByIdAndMemberId(Long id, Long memberId);
    
    // ✅ GOOD: @Query only for N+1 prevention
    @Query("SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.id = :id")
    Optional<Pet> findByIdWithBreed(@Param("id") Long id);
    
    @Query("SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.memberId = :memberId")
    List<Pet> findByMemberIdWithBreed(@Param("memberId") Long memberId);
    
    // ✅ GOOD: Pagination with fetch join
    @Query(value = "SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.memberId = :memberId",
           countQuery = "SELECT COUNT(p) FROM Pet p WHERE p.memberId = :memberId")
    Page<Pet> findByMemberIdWithBreed(@Param("memberId") Long memberId, Pageable pageable);
}
```

### Critical Reminders

1. **Use Fetch Join for relationships**: Prevent N+1 problems
2. **Method naming first**: Only use @Query when necessary
3. **Pageable + Fetch Join**: Provide separate countQuery
4. **Native queries**: Last resort only (breaks database portability)

## Value Objects (@Embeddable)

Use value objects to group related fields:

### Example: Location

```sql
-- DDL
place_name VARCHAR(200) NOT NULL,
latitude DECIMAL(10,8) NOT NULL,
longitude DECIMAL(11,8) NOT NULL,
address VARCHAR(500)
```

```java
// Value Object
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {
    @Column(nullable = false, length = 200)
    private String placeName;
    
    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(length = 500)
    private String address;
    
    @Builder
    public Location(String placeName, BigDecimal latitude, BigDecimal longitude, String address) {
        validateCoordinates(latitude, longitude);
        this.placeName = placeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }
    
    public double distanceTo(Location other) {
        // Haversine formula for distance calculation
        double lat1 = this.latitude.doubleValue();
        double lon1 = this.longitude.doubleValue();
        double lat2 = other.latitude.doubleValue();
        double lon2 = other.longitude.doubleValue();
        
        // ... calculation logic
        return distance;
    }
    
    private void validateCoordinates(BigDecimal lat, BigDecimal lon) {
        if (lat.compareTo(new BigDecimal("-90")) < 0 || 
            lat.compareTo(new BigDecimal("90")) > 0) {
            throw new BusinessException(ThreadErrorCode.INVALID_LATITUDE);
        }
        if (lon.compareTo(new BigDecimal("-180")) < 0 || 
            lon.compareTo(new BigDecimal("180")) > 0) {
            throw new BusinessException(ThreadErrorCode.INVALID_LONGITUDE);
        }
    }
}

// Entity
@Entity
public class Thread extends BaseTimeEntity {
    @Embedded
    private Location location;
}
```

### Example: MannerTemperature

```sql
-- DDL
manner_temperature DECIMAL(3,1) NOT NULL DEFAULT 5.0,
manner_score_sum INT NOT NULL DEFAULT 0,
manner_score_count INT NOT NULL DEFAULT 0
```

```java
// Value Object with business logic
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MannerTemperature {
    private static final BigDecimal MIN = BigDecimal.ONE;
    private static final BigDecimal MAX = BigDecimal.TEN;
    private static final BigDecimal DEFAULT = new BigDecimal("5.0");
    
    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal value = DEFAULT;
    
    @Column(nullable = false)
    private int scoreSum = 0;
    
    @Column(nullable = false)
    private int scoreCount = 0;
    
    public void addScore(int score) {
        validateScore(score);
        this.scoreSum += score;
        this.scoreCount += 1;
        this.value = calculateAverage();
    }
    
    private BigDecimal calculateAverage() {
        if (scoreCount == 0) {
            return DEFAULT;
        }
        BigDecimal avg = BigDecimal.valueOf(scoreSum)
            .divide(BigDecimal.valueOf(scoreCount), 1, RoundingMode.HALF_UP);
        
        // Clamp to [1.0, 10.0]
        if (avg.compareTo(MIN) < 0) return MIN;
        if (avg.compareTo(MAX) > 0) return MAX;
        return avg;
    }
    
    private void validateScore(int score) {
        if (score < 1 || score > 10) {
            throw new BusinessException(MemberErrorCode.INVALID_MANNER_SCORE);
        }
    }
}

// Entity
@Entity
public class Member extends BaseTimeEntity {
    @Embedded
    private MannerTemperature mannerTemperature = new MannerTemperature();
    
    public void addMannerScore(int score) {
        this.mannerTemperature.addScore(score);
    }
}
```

## Relationship Patterns

### Many-to-Many with Join Table

Use explicit join entity (preferred):

```sql
-- DDL
CREATE TABLE pet_personality (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pet_id BIGINT NOT NULL,
    personality_type_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pet_personality (pet_id, personality_type_id)
);
```

```java
// Join Entity
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"pet_id", "personality_type_id"})
})
public class PetPersonality extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personality_type_id", nullable = false)
    private PetPersonalityType personalityType;
    
    @Builder
    private PetPersonality(Pet pet, PetPersonalityType personalityType) {
        this.pet = pet;
        this.personalityType = personalityType;
    }
}

// Pet entity
@Entity
public class Pet extends BaseTimeEntity {
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PetPersonality> personalities = new ArrayList<>();
    
    public void addPersonality(PetPersonalityType type) {
        PetPersonality personality = PetPersonality.builder()
            .pet(this)
            .personalityType(type)
            .build();
        this.personalities.add(personality);
    }
    
    public void clearPersonalities() {
        this.personalities.clear();
    }
}
```

### One-to-Many

```java
@Entity
public class Post extends BaseTimeEntity {
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
    
    // Helper methods
    public void addComment(Comment comment) {
        this.comments.add(comment);
        comment.setPost(this);  // Maintain bidirectional consistency
    }
}

@Entity
public class Comment extends BaseTimeEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    void setPost(Post post) {  // package-private
        this.post = post;
    }
}
```

## Common Patterns

### Main Flag Pattern

For entities where only one can be "main":

```java
@Entity
public class Pet extends BaseTimeEntity {
    @Column(nullable = false)
    private boolean isMain = false;
    
    public void setAsMain() {
        this.isMain = true;
    }
    
    public void removeMainFlag() {
        this.isMain = false;
    }
}

// Service layer
@Transactional
public void setMainPet(Long memberId, Long petId) {
    // Remove existing main flag
    petRepository.findByMemberIdAndIsMainTrue(memberId)
        .ifPresent(Pet::removeMainFlag);
    
    // Set new main
    Pet pet = petRepository.findById(petId)...;
    pet.setAsMain();
}
```

### Status Transition Pattern

```java
@Entity
public class Thread extends BaseTimeEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThreadStatus status = ThreadStatus.ACTIVE;
    
    public void close() {
        if (this.status == ThreadStatus.CLOSED) {
            throw new BusinessException(ThreadErrorCode.ALREADY_CLOSED);
        }
        this.status = ThreadStatus.CLOSED;
    }
    
    public void reopen() {
        if (this.status != ThreadStatus.CLOSED) {
            throw new BusinessException(ThreadErrorCode.NOT_CLOSED);
        }
        this.status = ThreadStatus.ACTIVE;
    }
    
    public boolean isActive() {
        return this.status == ThreadStatus.ACTIVE;
    }
}

public enum ThreadStatus {
    ACTIVE, CLOSED, DELETED
}
```

### Counter Pattern

```java
@Entity
public class Thread extends BaseTimeEntity {
    @Column(nullable = false)
    private int currentParticipants = 0;
    
    @Column
    private Integer maxParticipants;  // Nullable for INDIVIDUAL type
    
    public void incrementParticipants() {
        if (maxParticipants != null && currentParticipants >= maxParticipants) {
            throw new BusinessException(ThreadErrorCode.MAX_PARTICIPANTS_EXCEEDED);
        }
        this.currentParticipants++;
    }
    
    public void decrementParticipants() {
        if (currentParticipants > 0) {
            this.currentParticipants--;
        }
    }
    
    public boolean isFull() {
        return maxParticipants != null && currentParticipants >= maxParticipants;
    }
}
```

## Complete Entity Example

```java
package com.ainiinu.pet.entity;

import com.ainiinu.common.entity.BaseTimeEntity;
import com.ainiinu.common.exception.BusinessException;
import com.ainiinu.pet.exception.PetErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ID reference to Member (different context)
    @Column(nullable = false)
    private Long memberId;
    
    // Relationship to Breed (same context)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", nullable = false)
    private Breed breed;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(nullable = false)
    private Integer age;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetSize size;
    
    @Column(length = 10)
    private String mbti;
    
    @Column(nullable = false)
    private Boolean isNeutered = false;
    
    @Column(nullable = false, length = 500)
    private String photoUrl;
    
    @Column(nullable = false)
    private Boolean isMain = false;
    
    @Column(length = 20)
    private String certificationNumber;
    
    @Column(nullable = false)
    private Boolean isCertified = false;
    
    // Many-to-many through join entity
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PetPersonality> personalities = new ArrayList<>();
    
    @Builder
    private Pet(Long memberId, Breed breed, String name, Integer age, Gender gender, 
                PetSize size, String photoUrl) {
        validateName(name);
        validateAge(age);
        this.memberId = memberId;
        this.breed = breed;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.size = size;
        this.photoUrl = photoUrl;
    }
    
    // Business methods
    public void updateInfo(String name, Integer age, String mbti, String photoUrl) {
        if (name != null) {
            validateName(name);
            this.name = name;
        }
        if (age != null) {
            validateAge(age);
            this.age = age;
        }
        if (mbti != null) {
            this.mbti = mbti;
        }
        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
    }
    
    public void setAsMain() {
        this.isMain = true;
    }
    
    public void removeMainFlag() {
        this.isMain = false;
    }
    
    public void certify(String certificationNumber) {
        this.certificationNumber = certificationNumber;
        this.isCertified = true;
    }
    
    // Validation methods
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(PetErrorCode.INVALID_PET_NAME);
        }
        if (name.length() > 10) {
            throw new BusinessException(PetErrorCode.PET_NAME_TOO_LONG);
        }
    }
    
    private void validateAge(Integer age) {
        if (age == null || age < 0 || age > 30) {
            throw new BusinessException(PetErrorCode.INVALID_PET_AGE);
        }
    }
    
    public enum Gender {
        MALE, FEMALE
    }
    
    public enum PetSize {
        SMALL, MEDIUM, LARGE
    }
}
```

## Best Practices Checklist

- [ ] Extends BaseTimeEntity
- [ ] @NoArgsConstructor(access = PROTECTED)
- [ ] No public setters
- [ ] Business methods with validation
- [ ] Lazy loading for @ManyToOne/@OneToOne
- [ ] ID-only references for other contexts
- [ ] @Embeddable for value objects
- [ ] EnumType.STRING for enums
- [ ] Builder for complex initialization
- [ ] Private validation methods