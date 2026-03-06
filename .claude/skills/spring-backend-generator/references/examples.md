# Complete Code Examples

This document provides complete, working examples of all layers for the 아이니이누 project.

## Example 1: Pet Context (Complete CRUD)

### 1. ErrorCode

`pet/exception/PetErrorCode.java`

```java
package com.ainiinu.pet.exception;

import com.ainiinu.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PetErrorCode implements ErrorCode {
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "반려견을 찾을 수 없습니다"),
    MAX_PET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "P002", "최대 10마리까지만 등록 가능합니다"),
    ALREADY_MAIN_PET_EXISTS(HttpStatus.BAD_REQUEST, "P003", "이미 메인 반려견이 존재합니다"),
    BREED_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "견종을 찾을 수 없습니다"),
    INVALID_PET_AGE(HttpStatus.BAD_REQUEST, "P005", "유효하지 않은 나이입니다"),
    NOT_PET_OWNER(HttpStatus.FORBIDDEN, "P006", "해당 반려견의 소유자가 아닙니다"),
    INVALID_PET_NAME(HttpStatus.BAD_REQUEST, "P007", "유효하지 않은 이름입니다"),
    PET_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "P008", "이름은 최대 10자입니다");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

### 2. Entity

`pet/entity/Pet.java`

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long memberId;  // ID-only reference to Member
    
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
    
    @Column(nullable = false, length = 500)
    private String photoUrl;
    
    @Column(nullable = false)
    private Boolean isMain = false;
    
    @Builder
    private Pet(Long memberId, Breed breed, String name, Integer age, 
                Gender gender, String photoUrl) {
        validateName(name);
        validateAge(age);
        this.memberId = memberId;
        this.breed = breed;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.photoUrl = photoUrl;
    }
    
    public void updateInfo(String name, Integer age, String photoUrl) {
        if (name != null) {
            validateName(name);
            this.name = name;
        }
        if (age != null) {
            validateAge(age);
            this.age = age;
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
}
```

`pet/entity/Breed.java`

```java
package com.ainiinu.pet.entity;

import com.ainiinu.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Breed extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Size size;
    
    public enum Size {
        SMALL, MEDIUM, LARGE
    }
}
```

### 3. Repository

`pet/repository/PetRepository.java`

```java
package com.ainiinu.pet.repository;

import com.ainiinu.pet.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    
    // ✅ Simple queries - method naming is enough
    List<Pet> findByMemberId(Long memberId);
    
    long countByMemberId(Long memberId);
    
    Optional<Pet> findByMemberIdAndIsMainTrue(Long memberId);
    
    boolean existsByIdAndMemberId(Long id, Long memberId);
    
    // ✅ @Query only for N+1 prevention (fetch join)
    @Query("SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.id = :id")
    Optional<Pet> findByIdWithBreed(@Param("id") Long id);
    
    @Query("SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.memberId = :memberId")
    List<Pet> findByMemberIdWithBreed(@Param("memberId") Long memberId);
    
    // ✅ Pagination with fetch join (needs countQuery)
    @Query(value = "SELECT p FROM Pet p JOIN FETCH p.breed WHERE p.memberId = :memberId",
           countQuery = "SELECT COUNT(p) FROM Pet p WHERE p.memberId = :memberId")
    Page<Pet> findByMemberIdWithBreed(@Param("memberId") Long memberId, Pageable pageable);
}
```

`pet/repository/BreedRepository.java`

```java
package com.ainiinu.pet.repository;

import com.ainiinu.pet.entity.Breed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BreedRepository extends JpaRepository<Breed, Long> {
    Optional<Breed> findByName(String name);
}
```

### 4. Service

`pet/service/PetService.java`

```java
package com.ainiinu.pet.service;

import com.ainiinu.common.exception.BusinessException;
import com.ainiinu.pet.dto.PetRegisterRequest;
import com.ainiinu.pet.dto.PetResponse;
import com.ainiinu.pet.dto.PetUpdateRequest;
import com.ainiinu.pet.entity.Breed;
import com.ainiinu.pet.entity.Pet;
import com.ainiinu.pet.exception.PetErrorCode;
import com.ainiinu.pet.repository.BreedRepository;
import com.ainiinu.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {
    
    private static final int MAX_PET_COUNT = 10;
    
    private final PetRepository petRepository;
    private final BreedRepository breedRepository;
    
    public PetResponse getPet(Long petId) {
        Pet pet = petRepository.findByIdWithBreed(petId)
            .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        
        return PetResponse.from(pet);
    }
    
    public List<PetResponse> getMyPets(Long memberId) {
        List<Pet> pets = petRepository.findByMemberIdWithBreed(memberId);
        return pets.stream()
            .map(PetResponse::from)
            .toList();
    }
    
    public Page<PetResponse> getMyPetsPaged(Long memberId, Pageable pageable) {
        Page<Pet> pets = petRepository.findByMemberIdWithBreed(memberId, pageable);
        return pets.map(PetResponse::from);
    }
    
    @Transactional
    public PetResponse registerPet(Long memberId, PetRegisterRequest request) {
        // Business rule: Max 10 pets per member
        long petCount = petRepository.countByMemberId(memberId);
        if (petCount >= MAX_PET_COUNT) {
            throw new BusinessException(PetErrorCode.MAX_PET_LIMIT_EXCEEDED);
        }
        
        Breed breed = breedRepository.findById(request.breedId())
            .orElseThrow(() -> new BusinessException(PetErrorCode.BREED_NOT_FOUND));
        
        Pet pet = Pet.builder()
            .memberId(memberId)
            .breed(breed)
            .name(request.name())
            .age(request.age())
            .gender(request.gender())
            .photoUrl(request.photoUrl())
            .build();
        
        Pet saved = petRepository.save(pet);
        
        log.info("Pet registered: petId={}, memberId={}, name={}", 
                saved.getId(), memberId, saved.getName());
        
        return PetResponse.from(saved);
    }
    
    @Transactional
    public PetResponse updatePet(Long memberId, Long petId, PetUpdateRequest request) {
        Pet pet = petRepository.findByIdWithBreed(petId)
            .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        
        // Check ownership
        if (!pet.getMemberId().equals(memberId)) {
            throw new BusinessException(PetErrorCode.NOT_PET_OWNER);
        }
        
        // Business method (Dirty Checking handles UPDATE)
        pet.updateInfo(request.name(), request.age(), request.photoUrl());
        
        log.info("Pet updated: petId={}, memberId={}", petId, memberId);
        
        return PetResponse.from(pet);
    }
    
    @Transactional
    public void deletePet(Long memberId, Long petId) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        
        if (!pet.getMemberId().equals(memberId)) {
            throw new BusinessException(PetErrorCode.NOT_PET_OWNER);
        }
        
        petRepository.delete(pet);  // Hard delete
        
        log.info("Pet deleted: petId={}, memberId={}", petId, memberId);
    }
    
    @Transactional
    public PetResponse setMainPet(Long memberId, Long petId) {
        Pet pet = petRepository.findByIdWithBreed(petId)
            .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        
        if (!pet.getMemberId().equals(memberId)) {
            throw new BusinessException(PetErrorCode.NOT_PET_OWNER);
        }
        
        // Remove existing main flag
        petRepository.findByMemberIdAndIsMainTrue(memberId)
            .ifPresent(Pet::removeMainFlag);
        
        // Set new main
        pet.setAsMain();
        
        log.info("Main pet changed: petId={}, memberId={}", petId, memberId);
        
        return PetResponse.from(pet);
    }
}
```

### 5. DTO

`pet/dto/PetRegisterRequest.java`

```java
package com.ainiinu.pet.dto;

import com.ainiinu.pet.entity.Pet;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PetRegisterRequest {
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 10, message = "이름은 최대 10자입니다")
    private String name;
    
    @NotNull(message = "나이는 필수입니다")
    @Min(value = 0, message = "나이는 0 이상이어야 합니다")
    @Max(value = 30, message = "나이는 30 이하여야 합니다")
    private Integer age;
    
    @NotNull(message = "성별은 필수입니다")
    private Pet.Gender gender;
    
    @NotNull(message = "견종은 필수입니다")
    private Long breedId;
    
    @NotBlank(message = "사진은 필수입니다")
    private String photoUrl;
}
```

`pet/dto/PetUpdateRequest.java`

```java
package com.ainiinu.pet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PetUpdateRequest {
    @Size(max = 10, message = "이름은 최대 10자입니다")
    private String name;
    
    @Min(value = 0, message = "나이는 0 이상이어야 합니다")
    @Max(value = 30, message = "나이는 30 이하여야 합니다")
    private Integer age;
    
    private String photoUrl;
}
```

`pet/dto/PetResponse.java`

```java
package com.ainiinu.pet.dto;

import com.ainiinu.pet.entity.Pet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PetResponse {
    private Long id;
    private String name;
    private Integer age;
    private String gender;
    private String breedName;
    private String photoUrl;
    private Boolean isMain;
    
    public static PetResponse from(Pet pet) {
        return PetResponse.builder()
            .id(pet.getId())
            .name(pet.getName())
            .age(pet.getAge())
            .gender(pet.getGender().name())
            .breedName(pet.getBreed().getName())
            .photoUrl(pet.getPhotoUrl())
            .isMain(pet.getIsMain())
            .build();
    }
}
```

### 6. Controller

`pet/controller/PetController.java`

무한 스크롤 목록 API는 `Slice` + `SliceResponse`를 사용합니다. (`SliceResponse`는 추후 추가 예정)

```java
package com.ainiinu.pet.controller;

import com.ainiinu.common.response.ApiResponse;
import com.ainiinu.common.response.PageResponse;
import com.ainiinu.pet.dto.PetRegisterRequest;
import com.ainiinu.pet.dto.PetResponse;
import com.ainiinu.pet.dto.PetUpdateRequest;
import com.ainiinu.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/pets")
public class PetController {
    
    private final PetService petService;
    
    /**
     * 반려견 단건 조회
     */
    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@PathVariable Long petId) {
        PetResponse response = petService.getPet(petId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 내 반려견 목록 조회 (전체)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getMyPets(
            @RequestParam Long memberId) {
        List<PetResponse> responses = petService.getMyPets(memberId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    /**
     * 내 반려견 목록 조회 (페이지네이션)
     */
    @GetMapping("/my/paged")
    public ResponseEntity<ApiResponse<PageResponse<PetResponse>>> getMyPetsPaged(
            @RequestParam Long memberId,
            Pageable pageable) {
        Page<PetResponse> page = petService.getMyPetsPaged(memberId, pageable);
        PageResponse<PetResponse> response = PageResponse.of(page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 반려견 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponse>> registerPet(
            @RequestParam Long memberId,
            @Valid @RequestBody PetRegisterRequest request) {
        PetResponse response = petService.registerPet(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 반려견 수정
     */
    @PutMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(
            @RequestParam Long memberId,
            @PathVariable Long petId,
            @Valid @RequestBody PetUpdateRequest request) {
        PetResponse response = petService.updatePet(memberId, petId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 반려견 삭제
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<ApiResponse<Void>> deletePet(
            @RequestParam Long memberId,
            @PathVariable Long petId) {
        petService.deletePet(memberId, petId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 메인 반려견 설정
     */
    @PatchMapping("/{petId}/main")
    public ResponseEntity<ApiResponse<PetResponse>> setMainPet(
            @RequestParam Long memberId,
            @PathVariable Long petId) {
        PetResponse response = petService.setMainPet(memberId, petId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

## Example 2: Member Context with Value Object

### Entity with Value Object

`member/entity/Member.java`

```java
package com.ainiinu.member.entity;

import com.ainiinu.common.entity.BaseTimeEntity;
import com.ainiinu.common.exception.BusinessException;
import com.ainiinu.member.entity.vo.MannerTemperature;
import com.ainiinu.member.exception.MemberErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberType memberType = MemberType.NON_PET_OWNER;
    
    @Embedded
    private MannerTemperature mannerTemperature = new MannerTemperature();
    
    @Builder
    private Member(String email, String nickname) {
        validateEmail(email);
        validateNickname(nickname);
        this.email = email;
        this.nickname = nickname;
    }
    
    public void updateNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }
    
    public void addMannerScore(int score) {
        this.mannerTemperature.addScore(score);
    }
    
    public void convertToPetOwner() {
        this.memberType = MemberType.PET_OWNER;
    }
    
    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new BusinessException(MemberErrorCode.INVALID_EMAIL);
        }
    }
    
    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new BusinessException(MemberErrorCode.INVALID_NICKNAME);
        }
        if (nickname.length() > 10) {
            throw new BusinessException(MemberErrorCode.NICKNAME_TOO_LONG);
        }
    }
    
    public enum MemberType {
        PET_OWNER, NON_PET_OWNER
    }
}
```

`member/entity/vo/MannerTemperature.java`

```java
package com.ainiinu.member.entity.vo;

import com.ainiinu.common.exception.BusinessException;
import com.ainiinu.member.exception.MemberErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MannerTemperature {
    
    private static final BigDecimal MIN = BigDecimal.ONE;
    private static final BigDecimal MAX = BigDecimal.TEN;
    private static final BigDecimal DEFAULT = new BigDecimal("5.0");
    
    @Column(name = "manner_temperature", nullable = false, precision = 3, scale = 1)
    private BigDecimal value = DEFAULT;
    
    @Column(name = "manner_score_sum", nullable = false)
    private Integer scoreSum = 0;
    
    @Column(name = "manner_score_count", nullable = false)
    private Integer scoreCount = 0;
    
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
```

## Example 3: Testing

`pet/service/PetServiceTest.java`

```java
package com.ainiinu.pet.service;

import com.ainiinu.common.exception.BusinessException;
import com.ainiinu.pet.dto.PetRegisterRequest;
import com.ainiinu.pet.dto.PetResponse;
import com.ainiinu.pet.entity.Breed;
import com.ainiinu.pet.entity.Pet;
import com.ainiinu.pet.exception.PetErrorCode;
import com.ainiinu.pet.repository.BreedRepository;
import com.ainiinu.pet.repository.PetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {
    
    @Mock
    private PetRepository petRepository;
    
    @Mock
    private BreedRepository breedRepository;
    
    @InjectMocks
    private PetService petService;
    
    @DisplayName("반려견 등록 - 성공")
    @Test
    void registerPet_shouldReturnPetResponse_whenValid() {
        // given
        Long memberId = 1L;
        Long breedId = 1L;
        
        PetRegisterRequest request = new PetRegisterRequest();
        request.setName("몽이");
        request.setAge(3);
        request.setGender(Pet.Gender.MALE);
        request.setBreedId(breedId);
        request.setPhotoUrl("photo.jpg");
        
        Breed breed = Breed.builder()
            .id(breedId)
            .name("푸들")
            .build();
        
        Pet pet = Pet.builder()
            .memberId(memberId)
            .breed(breed)
            .name("몽이")
            .age(3)
            .gender(Pet.Gender.MALE)
            .photoUrl("photo.jpg")
            .build();
        
        given(petRepository.countByMemberId(memberId)).willReturn(5L);
        given(breedRepository.findById(breedId)).willReturn(Optional.of(breed));
        given(petRepository.save(any(Pet.class))).willReturn(pet);
        
        // when
        PetResponse response = petService.registerPet(memberId, request);
        
        // then
        assertThat(response.getName()).isEqualTo("몽이");
        assertThat(response.getAge()).isEqualTo(3);
        assertThat(response.getBreedName()).isEqualTo("푸들");
        verify(petRepository).save(any(Pet.class));
    }
    
    @DisplayName("반려견 등록 - 최대 개수 초과 시 예외")
    @Test
    void registerPet_shouldThrowException_whenExceedsMaxCount() {
        // given
        Long memberId = 1L;
        
        PetRegisterRequest request = new PetRegisterRequest();
        request.setName("몽이");
        request.setAge(3);
        request.setGender(Pet.Gender.MALE);
        request.setBreedId(1L);
        request.setPhotoUrl("photo.jpg");
        
        given(petRepository.countByMemberId(memberId)).willReturn(10L);
        
        // when & then
        assertThatThrownBy(() -> petService.registerPet(memberId, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining(PetErrorCode.MAX_PET_LIMIT_EXCEEDED.getMessage());
    }
}
```

## Summary

This examples file demonstrates:

1. **Complete context structure** - All layers from ErrorCode to Controller
2. **DDD principles** - ID-only references, value objects, business methods
3. **Proper annotations** - Lombok, JPA, validation (JPA standard only, no Hibernate-specific)
4. **Transaction management** - @Transactional(readOnly=true) for queries
5. **Error handling** - Domain-specific ErrorCode enums
6. **Response patterns** - ResponseEntity<ApiResponse<T>>
7. **N+1 prevention** - JOIN FETCH only when needed
8. **Repository patterns** - Method naming first, @Query only for complex cases
9. **Testing** - BDD style with given/when/then

Use these examples as templates when generating code for other contexts.
