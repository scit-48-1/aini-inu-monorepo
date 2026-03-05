# Unit Test Patterns

Service 레이어 단위 테스트 패턴입니다.

## 기본 구조

```java
package scit.ainiinu.pet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private PetService petService;

    // 테스트 메서드들...
}
```

## Mock 설정 패턴

### 1. 단일 값 반환

```java
@Test
@DisplayName("ID로 반려동물을 조회하면 정보를 반환한다")
void findById_Success() {
    // given
    Long petId = 1L;
    Pet pet = createPet(petId, "뽀삐", "포메라니안");
    given(petRepository.findById(petId)).willReturn(Optional.of(pet));

    // when
    PetResponse response = petService.findById(petId);

    // then
    assertThat(response.getId()).isEqualTo(petId);
    assertThat(response.getName()).isEqualTo("뽀삐");
}
```

### 2. 빈 Optional 반환 (Not Found)

```java
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
```

### 3. 리스트 반환

```java
@Test
@DisplayName("회원의 모든 반려동물을 조회한다")
void findAllByMemberId_Success() {
    // given
    Long memberId = 1L;
    List<Pet> pets = List.of(
        createPet(1L, "뽀삐", "포메라니안"),
        createPet(2L, "초코", "푸들")
    );
    given(petRepository.findAllByMemberId(memberId)).willReturn(pets);

    // when
    List<PetResponse> responses = petService.findAllByMemberId(memberId);

    // then
    assertThat(responses).hasSize(2);
    assertThat(responses).extracting("name")
        .containsExactly("뽀삐", "초코");
}
```

### 4. save() 호출 검증

```java
@Test
@DisplayName("반려동물을 등록하면 저장소에 저장된다")
void createPet_Success() {
    // given
    Long memberId = 1L;
    PetCreateRequest request = new PetCreateRequest();
    request.setName("뽀삐");
    request.setBreed("포메라니안");

    Pet savedPet = createPet(1L, "뽀삐", "포메라니안");
    given(petRepository.save(any(Pet.class))).willReturn(savedPet);

    // when
    PetResponse response = petService.createPet(memberId, request);

    // then
    assertThat(response.getName()).isEqualTo("뽀삐");
    then(petRepository).should().save(any(Pet.class));
}
```

### 5. ArgumentCaptor 사용

저장되는 객체의 내용을 검증할 때:

```java
@Test
@DisplayName("반려동물 등록 시 회원 ID가 올바르게 설정된다")
void createPet_SetsMemberIdCorrectly() {
    // given
    Long memberId = 1L;
    PetCreateRequest request = new PetCreateRequest();
    request.setName("뽀삐");

    ArgumentCaptor<Pet> petCaptor = ArgumentCaptor.forClass(Pet.class);
    given(petRepository.save(petCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

    // when
    petService.createPet(memberId, request);

    // then
    Pet capturedPet = petCaptor.getValue();
    assertThat(capturedPet.getMemberId()).isEqualTo(memberId);
    assertThat(capturedPet.getName()).isEqualTo("뽀삐");
}
```

## 예외 테스트 패턴

### 1. 기본 예외 검증

```java
@Test
@DisplayName("존재하지 않는 반려동물을 수정하면 예외가 발생한다")
void updatePet_NotFound_ThrowsException() {
    // given
    Long petId = 999L;
    PetUpdateRequest request = new PetUpdateRequest();
    given(petRepository.findById(petId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> petService.updatePet(petId, request))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
}
```

### 2. 예외 메시지 검증

```java
@Test
@DisplayName("유효하지 않은 나이로 등록하면 예외가 발생한다")
void createPet_InvalidAge_ThrowsException() {
    // given
    PetCreateRequest request = new PetCreateRequest();
    request.setAge(-1);

    // when & then
    assertThatThrownBy(() -> petService.createPet(1L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("나이는 0 이상이어야 합니다");
}
```

### 3. 예외가 발생하지 않음을 검증

```java
@Test
@DisplayName("유효한 정보로 등록하면 예외가 발생하지 않는다")
void createPet_ValidInfo_NoException() {
    // given
    PetCreateRequest request = createValidRequest();
    given(petRepository.save(any())).willReturn(createPet(1L, "뽀삐", "포메라니안"));

    // when & then
    assertThatCode(() -> petService.createPet(1L, request))
        .doesNotThrowAnyException();
}
```

## 권한 검증 패턴

```java
@Nested
@DisplayName("반려동물 수정")
class UpdatePet {

    @Test
    @DisplayName("소유자가 아닌 회원이 수정하면 예외가 발생한다")
    void failWhenNotOwner() {
        // given
        Long petId = 1L;
        Long ownerId = 1L;
        Long requesterId = 2L;  // 다른 회원

        Pet pet = createPet(petId, "뽀삐", "포메라니안");
        setMemberId(pet, ownerId);
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        PetUpdateRequest request = new PetUpdateRequest();

        // when & then
        assertThatThrownBy(() -> petService.updatePet(requesterId, petId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_PET_OWNER);
    }

    @Test
    @DisplayName("소유자가 수정하면 성공한다")
    void successWhenOwner() {
        // given
        Long petId = 1L;
        Long ownerId = 1L;

        Pet pet = createPet(petId, "뽀삐", "포메라니안");
        setMemberId(pet, ownerId);
        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        PetUpdateRequest request = new PetUpdateRequest();
        request.setName("뽀삐2");

        // when
        PetResponse response = petService.updatePet(ownerId, petId, request);

        // then
        assertThat(response.getName()).isEqualTo("뽀삐2");
    }
}
```

## 호출 검증 패턴

### 1. 호출 횟수 검증

```java
@Test
@DisplayName("반려동물을 삭제하면 deleteById가 호출된다")
void deletePet_CallsRepository() {
    // given
    Long petId = 1L;
    Pet pet = createPet(petId, "뽀삐", "포메라니안");
    given(petRepository.findById(petId)).willReturn(Optional.of(pet));

    // when
    petService.deletePet(petId);

    // then
    then(petRepository).should(times(1)).delete(pet);
}
```

### 2. 호출되지 않음 검증

```java
@Test
@DisplayName("조회 실패 시 저장 메서드는 호출되지 않는다")
void findById_NotFound_DoesNotSave() {
    // given
    given(petRepository.findById(anyLong())).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> petService.findById(1L));

    then(petRepository).should(never()).save(any());
}
```

## 헬퍼 메서드 패턴

테스트 클래스 내에 private 헬퍼 메서드를 정의합니다:

```java
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @InjectMocks
    private PetService petService;

    // 테스트 데이터 생성 헬퍼
    private Pet createPet(Long id, String name, String breed) {
        Pet pet = Pet.builder()
            .name(name)
            .breed(breed)
            .build();
        setId(pet, id);
        return pet;
    }

    // 리플렉션으로 ID 설정
    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setMemberId(Pet pet, Long memberId) {
        try {
            Field field = Pet.class.getDeclaredField("memberId");
            field.setAccessible(true);
            field.set(pet, memberId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PetCreateRequest createValidRequest() {
        PetCreateRequest request = new PetCreateRequest();
        request.setName("뽀삐");
        request.setBreed("포메라니안");
        request.setAge(3);
        return request;
    }

    // 테스트 메서드들...
}
```

## 다중 의존성 테스트

```java
@ExtendWith(MockitoExtension.class)
class WalkServiceTest {

    @Mock
    private WalkRepository walkRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private WalkService walkService;

    @Test
    @DisplayName("산책 시작 시 반려동물과 회원 정보를 모두 검증한다")
    void startWalk_ValidatesAll() {
        // given
        Long memberId = 1L;
        Long petId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(createMember(memberId)));
        given(petRepository.findById(petId)).willReturn(Optional.of(createPet(petId)));
        given(walkRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        WalkResponse response = walkService.startWalk(memberId, petId);

        // then
        then(memberRepository).should().findById(memberId);
        then(petRepository).should().findById(petId);
        then(walkRepository).should().save(any(Walk.class));
    }
}
```
