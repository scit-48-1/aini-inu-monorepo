# Slice Test Patterns

Controller (@WebMvcTest)와 Repository (@DataJpaTest) 슬라이스 테스트 패턴입니다.

## Controller 테스트 (@WebMvcTest)

### 기본 구조

```java
package scit.ainiinu.pet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PetController.class)
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PetService petService;

    // 테스트 메서드들...
}
```

### GET 요청 테스트

```java
@Nested
@DisplayName("반려동물 조회 API")
class FindPet {

    @Test
    @DisplayName("ID로 조회하면 반려동물 정보를 반환한다")
    @WithMockUser
    void findById_Success() throws Exception {
        // given
        Long petId = 1L;
        PetResponse response = PetResponse.builder()
            .id(petId)
            .name("뽀삐")
            .breed("포메라니안")
            .build();
        given(petService.findById(petId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/pets/{petId}", petId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(petId))
            .andExpect(jsonPath("$.data.name").value("뽀삐"))
            .andExpect(jsonPath("$.data.breed").value("포메라니안"));
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 404를 반환한다")
    @WithMockUser
    void findById_NotFound() throws Exception {
        // given
        Long petId = 999L;
        given(petService.findById(petId))
            .willThrow(new BusinessException(PetErrorCode.PET_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/pets/{petId}", petId))
            .andDo(print())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));
    }
}
```

### POST 요청 테스트

```java
@Nested
@DisplayName("반려동물 등록 API")
class CreatePet {

    @Test
    @DisplayName("유효한 정보로 등록하면 201을 반환한다")
    @WithMockUser
    void createPet_Success() throws Exception {
        // given
        PetCreateRequest request = new PetCreateRequest();
        request.setName("뽀삐");
        request.setBreed("포메라니안");

        PetResponse response = PetResponse.builder()
            .id(1L)
            .name("뽀삐")
            .breed("포메라니안")
            .build();
        given(petService.createPet(anyLong(), any(PetCreateRequest.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/pets")
                .with(csrf())  // CSRF 토큰 필요
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.name").value("뽀삐"));
    }

    @Test
    @DisplayName("이름이 없으면 400을 반환한다")
    @WithMockUser
    void createPet_WithoutName_BadRequest() throws Exception {
        // given
        PetCreateRequest request = new PetCreateRequest();
        // name 없음

        // when & then
        mockMvc.perform(post("/api/pets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }
}
```

### PUT 요청 테스트

```java
@Nested
@DisplayName("반려동물 수정 API")
class UpdatePet {

    @Test
    @DisplayName("유효한 정보로 수정하면 200을 반환한다")
    @WithMockUser
    void updatePet_Success() throws Exception {
        // given
        Long petId = 1L;
        PetUpdateRequest request = new PetUpdateRequest();
        request.setName("뽀삐2");

        PetResponse response = PetResponse.builder()
            .id(petId)
            .name("뽀삐2")
            .build();
        given(petService.updatePet(anyLong(), eq(petId), any(PetUpdateRequest.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/pets/{petId}", petId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("뽀삐2"));
    }
}
```

### DELETE 요청 테스트

```java
@Nested
@DisplayName("반려동물 삭제 API")
class DeletePet {

    @Test
    @DisplayName("삭제하면 204를 반환한다")
    @WithMockUser
    void deletePet_Success() throws Exception {
        // given
        Long petId = 1L;
        willDoNothing().given(petService).deletePet(anyLong(), eq(petId));

        // when & then
        mockMvc.perform(delete("/api/pets/{petId}", petId)
                .with(csrf()))
            .andDo(print())
            .andExpect(status().isNoContent());

        then(petService).should().deletePet(anyLong(), eq(petId));
    }
}
```

### 인증 테스트

```java
@Nested
@DisplayName("인증")
class Authentication {

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void withoutAuth_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/pets/1"))
            .andDo(print())
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증된 사용자는 접근할 수 있다")
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    void withAuth_Success() throws Exception {
        // given
        given(petService.findById(1L)).willReturn(PetResponse.builder().id(1L).build());

        // when & then
        mockMvc.perform(get("/api/pets/1"))
            .andDo(print())
            .andExpect(status().isOk());
    }
}
```

### 페이징 응답 테스트

```java
@Test
@DisplayName("반려동물 목록을 페이징하여 조회한다")
@WithMockUser
void findAll_WithPaging() throws Exception {
    // given
    List<PetResponse> pets = List.of(
        PetResponse.builder().id(1L).name("뽀삐").build(),
        PetResponse.builder().id(2L).name("초코").build()
    );
    SliceResponse<PetResponse> sliceResponse = SliceResponse.of(
        pets, false, 0, 10
    );
    given(petService.findAll(any(Pageable.class))).willReturn(sliceResponse);

    // when & then
    mockMvc.perform(get("/api/pets")
            .param("page", "0")
            .param("size", "10"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.hasNext").value(false));
}
```

---

## Repository 테스트 (@DataJpaTest)

### 기본 구조

```java
package scit.ainiinu.pet.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class PetRepositoryTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private TestEntityManager entityManager;

    // 테스트 메서드들...
}
```

### 기본 CRUD 테스트

```java
@Nested
@DisplayName("저장")
class Save {

    @Test
    @DisplayName("반려동물을 저장하면 ID가 생성된다")
    void save_GeneratesId() {
        // given
        Pet pet = Pet.builder()
            .memberId(1L)
            .name("뽀삐")
            .breed("포메라니안")
            .build();

        // when
        Pet savedPet = petRepository.save(pet);

        // then
        assertThat(savedPet.getId()).isNotNull();
        assertThat(savedPet.getName()).isEqualTo("뽀삐");
    }
}

@Nested
@DisplayName("조회")
class Find {

    @Test
    @DisplayName("ID로 조회하면 반려동물을 반환한다")
    void findById_Success() {
        // given
        Pet pet = Pet.builder()
            .memberId(1L)
            .name("뽀삐")
            .breed("포메라니안")
            .build();
        entityManager.persist(pet);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Pet> foundPet = petRepository.findById(pet.getId());

        // then
        assertThat(foundPet).isPresent();
        assertThat(foundPet.get().getName()).isEqualTo("뽀삐");
    }
}
```

### 커스텀 쿼리 메서드 테스트

```java
@Nested
@DisplayName("회원별 반려동물 조회")
class FindByMemberId {

    @Test
    @DisplayName("회원 ID로 반려동물 목록을 조회한다")
    void findAllByMemberId_Success() {
        // given
        Long memberId = 1L;

        Pet pet1 = Pet.builder().memberId(memberId).name("뽀삐").build();
        Pet pet2 = Pet.builder().memberId(memberId).name("초코").build();
        Pet pet3 = Pet.builder().memberId(2L).name("다른회원펫").build();

        entityManager.persist(pet1);
        entityManager.persist(pet2);
        entityManager.persist(pet3);
        entityManager.flush();
        entityManager.clear();

        // when
        List<Pet> pets = petRepository.findAllByMemberId(memberId);

        // then
        assertThat(pets).hasSize(2);
        assertThat(pets).extracting("name")
            .containsExactlyInAnyOrder("뽀삐", "초코");
    }

    @Test
    @DisplayName("반려동물이 없으면 빈 리스트를 반환한다")
    void findAllByMemberId_Empty() {
        // given
        Long memberId = 999L;

        // when
        List<Pet> pets = petRepository.findAllByMemberId(memberId);

        // then
        assertThat(pets).isEmpty();
    }
}
```

### Soft Delete 테스트

```java
@Nested
@DisplayName("소프트 삭제")
class SoftDelete {

    @Test
    @DisplayName("삭제하면 deletedAt이 설정된다")
    void delete_SetsDeletedAt() {
        // given
        Pet pet = Pet.builder()
            .memberId(1L)
            .name("뽀삐")
            .build();
        entityManager.persist(pet);
        entityManager.flush();

        // when
        petRepository.delete(pet);
        entityManager.flush();
        entityManager.clear();

        // then - 네이티브 쿼리로 실제 데이터 확인
        Pet deletedPet = entityManager.find(Pet.class, pet.getId());
        // @SQLDelete로 soft delete 구현 시 deletedAt 확인
        // 실제 삭제 시 null 확인
    }

    @Test
    @DisplayName("삭제된 반려동물은 조회되지 않는다")
    void findById_ExcludesDeleted() {
        // given
        Pet pet = Pet.builder()
            .memberId(1L)
            .name("뽀삐")
            .build();
        entityManager.persist(pet);
        entityManager.flush();

        petRepository.delete(pet);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Pet> foundPet = petRepository.findById(pet.getId());

        // then
        assertThat(foundPet).isEmpty();  // @Where 어노테이션으로 필터링됨
    }
}
```

### 페이징 테스트

```java
@Nested
@DisplayName("페이징 조회")
class Paging {

    @Test
    @DisplayName("페이지별로 반려동물을 조회한다")
    void findAll_WithPaging() {
        // given
        for (int i = 1; i <= 15; i++) {
            Pet pet = Pet.builder()
                .memberId(1L)
                .name("펫" + i)
                .build();
            entityManager.persist(pet);
        }
        entityManager.flush();
        entityManager.clear();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Slice<Pet> petSlice = petRepository.findAllByMemberId(1L, pageable);

        // then
        assertThat(petSlice.getContent()).hasSize(10);
        assertThat(petSlice.hasNext()).isTrue();
    }
}
```

### 연관 엔티티 테스트

```java
@Nested
@DisplayName("연관 엔티티")
class Associations {

    @Test
    @DisplayName("반려동물과 함께 산책 기록을 조회한다")
    void findWithWalks() {
        // given
        Pet pet = Pet.builder()
            .memberId(1L)
            .name("뽀삐")
            .build();
        entityManager.persist(pet);

        Walk walk1 = Walk.builder().pet(pet).distance(1000).build();
        Walk walk2 = Walk.builder().pet(pet).distance(2000).build();
        entityManager.persist(walk1);
        entityManager.persist(walk2);

        entityManager.flush();
        entityManager.clear();

        // when
        Pet foundPet = petRepository.findWithWalksById(pet.getId())
            .orElseThrow();

        // then
        assertThat(foundPet.getWalks()).hasSize(2);
        assertThat(foundPet.getWalks())
            .extracting("distance")
            .containsExactlyInAnyOrder(1000, 2000);
    }
}
```

### TestEntityManager 사용 팁

```java
// persist - 영속화 (ID 생성 전)
entityManager.persist(entity);

// persistAndFlush - 영속화 + 즉시 DB 반영
entityManager.persistAndFlush(entity);

// flush - 영속성 컨텍스트 변경사항 DB 반영
entityManager.flush();

// clear - 영속성 컨텍스트 초기화 (1차 캐시 비움)
entityManager.clear();

// find - 1차 캐시 무시하고 DB에서 조회
entityManager.find(Pet.class, petId);

// 일반적인 테스트 패턴
entityManager.persist(entity);
entityManager.flush();   // DB에 반영
entityManager.clear();   // 캐시 비움 → 이후 조회 시 DB에서 읽음
```
