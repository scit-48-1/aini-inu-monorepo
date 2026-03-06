# Test Examples

완전한 테스트 코드 예제입니다.

## 1. Service Unit Test (완전한 예제)

```java
package scit.ainiinu.pet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.pet.dto.PetCreateRequest;
import scit.ainiinu.pet.dto.PetResponse;
import scit.ainiinu.pet.dto.PetUpdateRequest;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.exception.PetErrorCode;
import scit.ainiinu.pet.repository.PetRepository;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
        @DisplayName("유효한 정보로 등록하면 성공한다")
        void success() {
            // given
            Long memberId = 1L;
            PetCreateRequest request = new PetCreateRequest();
            request.setName("뽀삐");
            request.setBreed("포메라니안");
            request.setAge(3);

            Pet savedPet = createPet(1L, memberId, "뽀삐", "포메라니안");
            given(petRepository.save(any(Pet.class))).willReturn(savedPet);

            // when
            PetResponse response = petService.createPet(memberId, request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("뽀삐");
            assertThat(response.getBreed()).isEqualTo("포메라니안");
            then(petRepository).should().save(any(Pet.class));
        }

        @Test
        @DisplayName("저장 시 회원 ID가 올바르게 설정된다")
        void setsMemberIdCorrectly() {
            // given
            Long memberId = 1L;
            PetCreateRequest request = new PetCreateRequest();
            request.setName("뽀삐");

            ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
            given(petRepository.save(petCaptor.capture())).willAnswer(inv -> {
                Pet pet = inv.getArgument(0);
                setId(pet, 1L);
                return pet;
            });

            // when
            petService.createPet(memberId, request);

            // then
            Pet capturedPet = petCaptor.getValue();
            assertThat(capturedPet.getMemberId()).isEqualTo(memberId);
        }
    }

    @Nested
    @DisplayName("반려동물 조회")
    class FindPet {

        @Test
        @DisplayName("ID로 조회하면 반려동물 정보를 반환한다")
        void findById_Success() {
            // given
            Long petId = 1L;
            Pet pet = createPet(petId, 1L, "뽀삐", "포메라니안");
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when
            PetResponse response = petService.findById(petId);

            // then
            assertThat(response.getId()).isEqualTo(petId);
            assertThat(response.getName()).isEqualTo("뽀삐");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        void findById_NotFound_ThrowsException() {
            // given
            Long petId = 999L;
            given(petRepository.findById(petId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> petService.findById(petId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
        }

        @Test
        @DisplayName("회원의 모든 반려동물을 조회한다")
        void findAllByMemberId_Success() {
            // given
            Long memberId = 1L;
            List<Pet> pets = List.of(
                createPet(1L, memberId, "뽀삐", "포메라니안"),
                createPet(2L, memberId, "초코", "푸들")
            );
            given(petRepository.findAllByMemberId(memberId)).willReturn(pets);

            // when
            List<PetResponse> responses = petService.findAllByMemberId(memberId);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting("name")
                .containsExactly("뽀삐", "초코");
        }
    }

    @Nested
    @DisplayName("반려동물 수정")
    class UpdatePet {

        @Test
        @DisplayName("소유자가 수정하면 성공한다")
        void success() {
            // given
            Long memberId = 1L;
            Long petId = 1L;
            Pet pet = createPet(petId, memberId, "뽀삐", "포메라니안");
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            PetUpdateRequest request = new PetUpdateRequest();
            request.setName("뽀삐2");

            // when
            PetResponse response = petService.updatePet(memberId, petId, request);

            // then
            assertThat(response.getName()).isEqualTo("뽀삐2");
        }

        @Test
        @DisplayName("소유자가 아니면 예외가 발생한다")
        void notOwner_ThrowsException() {
            // given
            Long ownerId = 1L;
            Long requesterId = 2L;
            Long petId = 1L;

            Pet pet = createPet(petId, ownerId, "뽀삐", "포메라니안");
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            PetUpdateRequest request = new PetUpdateRequest();

            // when & then
            assertThatThrownBy(() -> petService.updatePet(requesterId, petId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_PET_OWNER);
        }
    }

    @Nested
    @DisplayName("반려동물 삭제")
    class DeletePet {

        @Test
        @DisplayName("소유자가 삭제하면 성공한다")
        void success() {
            // given
            Long memberId = 1L;
            Long petId = 1L;
            Pet pet = createPet(petId, memberId, "뽀삐", "포메라니안");
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when
            petService.deletePet(memberId, petId);

            // then
            then(petRepository).should().delete(pet);
        }

        @Test
        @DisplayName("존재하지 않는 반려동물을 삭제하면 예외가 발생한다")
        void notFound_ThrowsException() {
            // given
            Long petId = 999L;
            given(petRepository.findById(petId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> petService.deletePet(1L, petId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);

            then(petRepository).should(never()).delete(any());
        }
    }

    // 헬퍼 메서드
    private Pet createPet(Long id, Long memberId, String name, String breed) {
        Pet pet = Pet.builder()
            .memberId(memberId)
            .name(name)
            .breed(breed)
            .build();
        setId(pet, id);
        return pet;
    }

    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

## 2. Controller Slice Test (완전한 예제)

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
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.pet.dto.PetCreateRequest;
import scit.ainiinu.pet.dto.PetResponse;
import scit.ainiinu.pet.dto.PetUpdateRequest;
import scit.ainiinu.pet.exception.PetErrorCode;
import scit.ainiinu.pet.service.PetService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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

    @Nested
    @DisplayName("POST /api/pets - 반려동물 등록")
    class CreatePet {

        @Test
        @DisplayName("유효한 정보로 등록하면 201을 반환한다")
        @WithMockUser(username = "1")
        void success() throws Exception {
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
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("뽀삐"))
                .andExpect(jsonPath("$.data.breed").value("포메라니안"));
        }

        @Test
        @DisplayName("이름이 없으면 400을 반환한다")
        @WithMockUser(username = "1")
        void withoutName_BadRequest() throws Exception {
            // given
            PetCreateRequest request = new PetCreateRequest();
            request.setBreed("포메라니안");
            // name 없음

            // when & then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("인증 없이 요청하면 401을 반환한다")
        void unauthorized() throws Exception {
            // given
            PetCreateRequest request = new PetCreateRequest();
            request.setName("뽀삐");

            // when & then
            mockMvc.perform(post("/api/pets")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/pets/{petId} - 반려동물 조회")
    class FindPet {

        @Test
        @DisplayName("ID로 조회하면 반려동물 정보를 반환한다")
        @WithMockUser
        void success() throws Exception {
            // given
            Long petId = 1L;
            PetResponse response = PetResponse.builder()
                .id(petId)
                .name("뽀삐")
                .breed("포메라니안")
                .age(3)
                .build();

            given(petService.findById(petId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/pets/{petId}", petId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(petId))
                .andExpect(jsonPath("$.data.name").value("뽀삐"))
                .andExpect(jsonPath("$.data.breed").value("포메라니안"))
                .andExpect(jsonPath("$.data.age").value(3));
        }

        @Test
        @DisplayName("존재하지 않으면 404를 반환한다")
        @WithMockUser
        void notFound() throws Exception {
            // given
            Long petId = 999L;
            given(petService.findById(petId))
                .willThrow(new BusinessException(PetErrorCode.PET_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/pets/{petId}", petId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("PET_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/pets - 반려동물 목록 조회")
    class FindAllPets {

        @Test
        @DisplayName("회원의 반려동물 목록을 반환한다")
        @WithMockUser(username = "1")
        void success() throws Exception {
            // given
            List<PetResponse> responses = List.of(
                PetResponse.builder().id(1L).name("뽀삐").build(),
                PetResponse.builder().id(2L).name("초코").build()
            );

            given(petService.findAllByMemberId(1L)).willReturn(responses);

            // when & then
            mockMvc.perform(get("/api/pets"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("뽀삐"))
                .andExpect(jsonPath("$.data[1].name").value("초코"));
        }
    }

    @Nested
    @DisplayName("PUT /api/pets/{petId} - 반려동물 수정")
    class UpdatePet {

        @Test
        @DisplayName("유효한 정보로 수정하면 200을 반환한다")
        @WithMockUser(username = "1")
        void success() throws Exception {
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

        @Test
        @DisplayName("소유자가 아니면 403을 반환한다")
        @WithMockUser(username = "2")
        void notOwner_Forbidden() throws Exception {
            // given
            Long petId = 1L;
            PetUpdateRequest request = new PetUpdateRequest();
            request.setName("뽀삐2");

            given(petService.updatePet(anyLong(), eq(petId), any(PetUpdateRequest.class)))
                .willThrow(new BusinessException(PetErrorCode.NOT_PET_OWNER));

            // when & then
            mockMvc.perform(put("/api/pets/{petId}", petId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_PET_OWNER"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/pets/{petId} - 반려동물 삭제")
    class DeletePet {

        @Test
        @DisplayName("삭제하면 204를 반환한다")
        @WithMockUser(username = "1")
        void success() throws Exception {
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
}
```

---

## 3. Repository Slice Test (완전한 예제)

```java
package scit.ainiinu.pet.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import scit.ainiinu.pet.entity.Pet;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class PetRepositoryTest {

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("저장")
    class Save {

        @Test
        @DisplayName("반려동물을 저장하면 ID가 생성된다")
        void generatesId() {
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
            assertThat(savedPet.getBreed()).isEqualTo("포메라니안");
        }

        @Test
        @DisplayName("저장 시 생성일시가 자동 설정된다")
        void setsCreatedAt() {
            // given
            Pet pet = Pet.builder()
                .memberId(1L)
                .name("뽀삐")
                .build();

            // when
            Pet savedPet = petRepository.save(pet);
            entityManager.flush();

            // then
            assertThat(savedPet.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("조회")
    class Find {

        @Test
        @DisplayName("ID로 조회하면 반려동물을 반환한다")
        void findById() {
            // given
            Pet pet = Pet.builder()
                .memberId(1L)
                .name("뽀삐")
                .breed("포메라니안")
                .build();
            entityManager.persistAndFlush(pet);
            entityManager.clear();

            // when
            Optional<Pet> foundPet = petRepository.findById(pet.getId());

            // then
            assertThat(foundPet).isPresent();
            assertThat(foundPet.get().getName()).isEqualTo("뽀삐");
            assertThat(foundPet.get().getBreed()).isEqualTo("포메라니안");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
        void findById_NotFound() {
            // when
            Optional<Pet> foundPet = petRepository.findById(999L);

            // then
            assertThat(foundPet).isEmpty();
        }
    }

    @Nested
    @DisplayName("회원별 조회")
    class FindByMemberId {

        @Test
        @DisplayName("회원 ID로 반려동물 목록을 조회한다")
        void success() {
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
        void empty() {
            // when
            List<Pet> pets = petRepository.findAllByMemberId(999L);

            // then
            assertThat(pets).isEmpty();
        }
    }

    @Nested
    @DisplayName("페이징 조회")
    class Paging {

        @Test
        @DisplayName("페이지별로 반려동물을 조회한다")
        void findWithPaging() {
            // given
            Long memberId = 1L;
            for (int i = 1; i <= 15; i++) {
                Pet pet = Pet.builder()
                    .memberId(memberId)
                    .name("펫" + i)
                    .build();
                entityManager.persist(pet);
            }
            entityManager.flush();
            entityManager.clear();

            // when
            Slice<Pet> firstPage = petRepository.findAllByMemberId(memberId, PageRequest.of(0, 10));
            Slice<Pet> secondPage = petRepository.findAllByMemberId(memberId, PageRequest.of(1, 10));

            // then
            assertThat(firstPage.getContent()).hasSize(10);
            assertThat(firstPage.hasNext()).isTrue();

            assertThat(secondPage.getContent()).hasSize(5);
            assertThat(secondPage.hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("Dirty Checking으로 수정된다")
        void dirtyChecking() {
            // given
            Pet pet = Pet.builder()
                .memberId(1L)
                .name("뽀삐")
                .build();
            entityManager.persistAndFlush(pet);

            // when
            pet.updateName("뽀삐2");
            entityManager.flush();
            entityManager.clear();

            // then
            Pet updatedPet = petRepository.findById(pet.getId()).orElseThrow();
            assertThat(updatedPet.getName()).isEqualTo("뽀삐2");
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("삭제하면 조회되지 않는다")
        void delete() {
            // given
            Pet pet = Pet.builder()
                .memberId(1L)
                .name("뽀삐")
                .build();
            entityManager.persistAndFlush(pet);
            Long petId = pet.getId();

            // when
            petRepository.delete(pet);
            entityManager.flush();
            entityManager.clear();

            // then
            Optional<Pet> deletedPet = petRepository.findById(petId);
            assertThat(deletedPet).isEmpty();
        }
    }

    @Nested
    @DisplayName("커스텀 쿼리")
    class CustomQueries {

        @Test
        @DisplayName("이름으로 검색한다")
        void findByNameContaining() {
            // given
            Pet pet1 = Pet.builder().memberId(1L).name("뽀삐").build();
            Pet pet2 = Pet.builder().memberId(1L).name("뽀삐2").build();
            Pet pet3 = Pet.builder().memberId(1L).name("초코").build();

            entityManager.persist(pet1);
            entityManager.persist(pet2);
            entityManager.persist(pet3);
            entityManager.flush();
            entityManager.clear();

            // when
            List<Pet> pets = petRepository.findByNameContaining("뽀삐");

            // then
            assertThat(pets).hasSize(2);
            assertThat(pets).extracting("name")
                .containsExactlyInAnyOrder("뽀삐", "뽀삐2");
        }

        @Test
        @DisplayName("품종별로 조회한다")
        void findByBreed() {
            // given
            Pet pet1 = Pet.builder().memberId(1L).name("뽀삐").breed("포메라니안").build();
            Pet pet2 = Pet.builder().memberId(1L).name("초코").breed("푸들").build();
            Pet pet3 = Pet.builder().memberId(1L).name("몽이").breed("포메라니안").build();

            entityManager.persist(pet1);
            entityManager.persist(pet2);
            entityManager.persist(pet3);
            entityManager.flush();
            entityManager.clear();

            // when
            List<Pet> pomeranians = petRepository.findByBreed("포메라니안");

            // then
            assertThat(pomeranians).hasSize(2);
            assertThat(pomeranians).extracting("name")
                .containsExactlyInAnyOrder("뽀삐", "몽이");
        }
    }
}
```

---

## 4. Integration Test (완전한 예제)

```java
package scit.ainiinu.pet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.dto.PetCreateRequest;
import scit.ainiinu.pet.dto.PetResponse;
import scit.ainiinu.pet.dto.PetUpdateRequest;
import scit.ainiinu.pet.exception.PetErrorCode;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.pet.service.PetService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PetIntegrationTest {

    @Autowired
    private PetService petService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(
            Member.builder()
                .email("test@test.com")
                .nickname("테스터")
                .build()
        );
    }

    @Nested
    @DisplayName("반려동물 CRUD 전체 흐름")
    class CrudFlow {

        @Test
        @DisplayName("등록 → 조회 → 수정 → 삭제가 정상 동작한다")
        void fullFlow() {
            // 1. 등록
            PetCreateRequest createRequest = new PetCreateRequest();
            createRequest.setName("뽀삐");
            createRequest.setBreed("포메라니안");
            createRequest.setAge(3);

            PetResponse created = petService.createPet(testMember.getId(), createRequest);

            assertThat(created.getId()).isNotNull();
            assertThat(created.getName()).isEqualTo("뽀삐");
            assertThat(created.getBreed()).isEqualTo("포메라니안");

            // 2. 조회
            PetResponse found = petService.findById(created.getId());

            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getName()).isEqualTo("뽀삐");

            // 3. 수정
            PetUpdateRequest updateRequest = new PetUpdateRequest();
            updateRequest.setName("뽀삐2");
            updateRequest.setAge(4);

            PetResponse updated = petService.updatePet(
                testMember.getId(), created.getId(), updateRequest
            );

            assertThat(updated.getName()).isEqualTo("뽀삐2");
            assertThat(updated.getAge()).isEqualTo(4);

            // 4. 삭제
            petService.deletePet(testMember.getId(), created.getId());

            // 5. 삭제 확인
            assertThatThrownBy(() -> petService.findById(created.getId()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원별 반려동물 관리")
    class MemberPets {

        @Test
        @DisplayName("여러 반려동물을 등록하고 목록으로 조회한다")
        void createAndFindAll() {
            // given
            PetCreateRequest request1 = new PetCreateRequest();
            request1.setName("뽀삐");

            PetCreateRequest request2 = new PetCreateRequest();
            request2.setName("초코");

            petService.createPet(testMember.getId(), request1);
            petService.createPet(testMember.getId(), request2);

            // when
            List<PetResponse> pets = petService.findAllByMemberId(testMember.getId());

            // then
            assertThat(pets).hasSize(2);
            assertThat(pets).extracting("name")
                .containsExactlyInAnyOrder("뽀삐", "초코");
        }
    }

    @Nested
    @DisplayName("권한 검증")
    class Authorization {

        @Test
        @DisplayName("다른 회원의 반려동물을 수정하면 예외가 발생한다")
        void updateOtherMembersPet() {
            // given
            Member otherMember = memberRepository.save(
                Member.builder()
                    .email("other@test.com")
                    .nickname("다른사람")
                    .build()
            );

            PetCreateRequest request = new PetCreateRequest();
            request.setName("뽀삐");
            PetResponse pet = petService.createPet(testMember.getId(), request);

            PetUpdateRequest updateRequest = new PetUpdateRequest();
            updateRequest.setName("변경시도");

            // when & then
            assertThatThrownBy(() ->
                petService.updatePet(otherMember.getId(), pet.getId(), updateRequest)
            )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_PET_OWNER);
        }

        @Test
        @DisplayName("다른 회원의 반려동물을 삭제하면 예외가 발생한다")
        void deleteOtherMembersPet() {
            // given
            Member otherMember = memberRepository.save(
                Member.builder()
                    .email("other@test.com")
                    .nickname("다른사람")
                    .build()
            );

            PetCreateRequest request = new PetCreateRequest();
            request.setName("뽀삐");
            PetResponse pet = petService.createPet(testMember.getId(), request);

            // when & then
            assertThatThrownBy(() ->
                petService.deletePet(otherMember.getId(), pet.getId())
            )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_PET_OWNER);
        }
    }
}
```
