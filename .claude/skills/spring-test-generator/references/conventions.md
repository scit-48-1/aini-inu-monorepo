# Test Conventions

아이니이누 프로젝트의 테스트 코드 작성 규칙입니다.

## 1. 테스트 클래스 네이밍

```
{TargetClass}Test.java
```

예시:
- `PetService` → `PetServiceTest`
- `PetController` → `PetControllerTest`
- `PetRepository` → `PetRepositoryTest`

## 2. 패키지 구조

테스트 클래스는 소스 클래스와 동일한 패키지 구조를 따릅니다.

```
src/main/java/scit/ainiinu/pet/service/PetService.java
  ↓
src/test/java/scit/ainiinu/pet/service/PetServiceTest.java
```

## 3. BDD 스타일 (given/when/then)

모든 테스트는 BDD 스타일로 작성합니다.

```java
@Test
@DisplayName("유효한 정보로 반려동물을 등록하면 성공한다")
void createPet_WithValidInfo_Success() {
    // given - 테스트 데이터 준비
    Long memberId = 1L;
    PetCreateRequest request = new PetCreateRequest();
    request.setName("뽀삐");
    request.setBreed("포메라니안");

    Pet pet = Pet.create(memberId, request.getName(), request.getBreed());
    given(petRepository.save(any(Pet.class))).willReturn(pet);

    // when - 테스트 대상 실행
    PetResponse response = petService.createPet(memberId, request);

    // then - 결과 검증
    assertThat(response.getName()).isEqualTo("뽀삐");
    assertThat(response.getBreed()).isEqualTo("포메라니안");
    then(petRepository).should().save(any(Pet.class));
}
```

## 4. @DisplayName 규칙

- **한글로 작성**: 테스트 의도를 명확하게 전달
- **~하면 ~한다** 형식 권장
- 메서드명도 영어로 의미있게 작성

```java
// Good
@DisplayName("존재하지 않는 반려동물을 조회하면 예외가 발생한다")
void findById_WithNonExistentId_ThrowsException() { }

// Bad
@DisplayName("테스트1")
void test1() { }
```

## 5. @Nested 클래스 그룹핑

관련 테스트를 @Nested 클래스로 그룹화합니다.

```java
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Nested
    @DisplayName("반려동물 등록")
    class CreatePet {
        @Test
        @DisplayName("유효한 정보로 등록하면 성공한다")
        void success() { }

        @Test
        @DisplayName("이름이 없으면 예외가 발생한다")
        void failWithoutName() { }
    }

    @Nested
    @DisplayName("반려동물 조회")
    class FindPet {
        @Test
        @DisplayName("ID로 조회하면 반려동물 정보를 반환한다")
        void findById() { }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        void notFound() { }
    }
}
```

## 6. 테스트 데이터 생성

각 테스트에서 직접 생성합니다. 명확하고 이해하기 쉬운 데이터를 사용합니다.

```java
// given 섹션에서 테스트 데이터 직접 생성
Long memberId = 1L;
String petName = "뽀삐";
String breed = "포메라니안";

Pet pet = Pet.builder()
    .memberId(memberId)
    .name(petName)
    .breed(breed)
    .build();
```

### 리플렉션을 사용한 ID 설정

테스트에서 엔티티 ID가 필요한 경우:

```java
private void setId(Object entity, Long id) {
    try {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}

// 사용
Pet pet = Pet.create(memberId, "뽀삐", "포메라니안");
setId(pet, 1L);
```

## 7. Assertion 라이브러리

AssertJ를 사용합니다 (spring-boot-starter-test에 포함).

```java
import static org.assertj.core.api.Assertions.*;

// 값 검증
assertThat(result.getName()).isEqualTo("뽀삐");
assertThat(result.getAge()).isGreaterThan(0);
assertThat(results).hasSize(3);
assertThat(results).isEmpty();

// 예외 검증
assertThatThrownBy(() -> petService.findById(999L))
    .isInstanceOf(BusinessException.class)
    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);

// null 검증
assertThat(result).isNotNull();
assertThat(result.getDeletedAt()).isNull();
```

## 8. Mockito 사용 규칙

### BDDMockito 사용

```java
import static org.mockito.BDDMockito.*;

// given() 사용 (when() 대신)
given(petRepository.findById(1L)).willReturn(Optional.of(pet));
given(petRepository.save(any(Pet.class))).willReturn(pet);

// then().should() 사용 (verify() 대신)
then(petRepository).should().save(any(Pet.class));
then(petRepository).should(times(1)).findById(1L);
then(petRepository).should(never()).delete(any());
```

### Argument Matchers

```java
import static org.mockito.ArgumentMatchers.*;

any(Pet.class)
anyLong()
anyString()
eq(1L)
argThat(pet -> pet.getName().equals("뽀삐"))
```

## 9. 테스트 격리

- 각 테스트는 독립적으로 실행 가능해야 함
- 테스트 순서에 의존하지 않음
- @BeforeEach로 공통 설정, 단 필수적인 경우만

```java
@BeforeEach
void setUp() {
    // 정말 필요한 공통 설정만
}
```

## 10. 금지 사항

```java
// 금지: var 사용
var pet = petRepository.findById(1L);  // X
Pet pet = petRepository.findById(1L).orElseThrow();  // O

// 금지: 의미없는 테스트명
@Test
void test1() { }  // X

// 금지: BDD 스타일 미사용
@Test
void shouldCreatePet() {
    Pet pet = petService.create(...);  // given/when/then 없음
    assertNotNull(pet);
}

// 금지: JUnit 기본 assertion
assertEquals(expected, actual);  // X
assertThat(actual).isEqualTo(expected);  // O (AssertJ)
```

## 11. 테스트 어노테이션 요약

| 테스트 유형 | 어노테이션 | 용도 |
|------------|-----------|------|
| Unit (Service) | `@ExtendWith(MockitoExtension.class)` | 서비스 로직 단위 테스트 |
| Slice (Controller) | `@WebMvcTest(XxxController.class)` | 컨트롤러 레이어 테스트 |
| Slice (Repository) | `@DataJpaTest` | JPA 레포지토리 테스트 |
| Integration | `@SpringBootTest` | 전체 통합 테스트 |
| Security | `@WithMockUser` | 인증된 사용자 시뮬레이션 |
