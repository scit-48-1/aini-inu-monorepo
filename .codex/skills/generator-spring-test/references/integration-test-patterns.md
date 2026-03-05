# Integration Test Patterns

@SpringBootTest를 사용한 통합 테스트 패턴입니다.

## 언제 통합 테스트를 사용하는가

- **전체 흐름 검증**: Controller → Service → Repository → DB
- **트랜잭션 동작 검증**: 여러 서비스 간 트랜잭션 전파
- **실제 환경 시뮬레이션**: 실제 의존성 주입, 설정 적용
- **E2E 시나리오 테스트**: 사용자 시나리오 기반 테스트

## 기본 구조

```java
package scit.ainiinu.pet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional  // 테스트 후 롤백
class PetIntegrationTest {

    @Autowired
    private PetService petService;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private MemberRepository memberRepository;

    // 테스트 메서드들...
}
```

## 테스트 데이터 준비

### @BeforeEach 사용

```java
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
        // 테스트에 필요한 공통 데이터 준비
        testMember = Member.builder()
            .email("test@test.com")
            .nickname("테스터")
            .build();
        memberRepository.save(testMember);
    }

    @Test
    @DisplayName("반려동물을 등록하고 조회할 수 있다")
    void createAndFind() {
        // given
        PetCreateRequest request = new PetCreateRequest();
        request.setName("뽀삐");
        request.setBreed("포메라니안");

        // when
        PetResponse created = petService.createPet(testMember.getId(), request);
        PetResponse found = petService.findById(created.getId());

        // then
        assertThat(found.getName()).isEqualTo("뽀삐");
        assertThat(found.getBreed()).isEqualTo("포메라니안");
    }
}
```

### 각 테스트에서 직접 생성

```java
@Test
@DisplayName("회원의 모든 반려동물을 조회한다")
void findAllByMember() {
    // given
    Member member = memberRepository.save(
        Member.builder().email("test@test.com").nickname("테스터").build()
    );

    petRepository.save(Pet.builder().memberId(member.getId()).name("뽀삐").build());
    petRepository.save(Pet.builder().memberId(member.getId()).name("초코").build());

    // when
    List<PetResponse> pets = petService.findAllByMemberId(member.getId());

    // then
    assertThat(pets).hasSize(2);
    assertThat(pets).extracting("name").containsExactlyInAnyOrder("뽀삐", "초코");
}
```

## 전체 흐름 테스트

### CRUD 전체 흐름

```java
@Nested
@DisplayName("반려동물 CRUD 전체 흐름")
class PetCrudFlow {

    @Test
    @DisplayName("등록 → 조회 → 수정 → 삭제가 정상 동작한다")
    void fullCrudFlow() {
        // given - 회원 생성
        Member member = memberRepository.save(
            Member.builder().email("test@test.com").nickname("테스터").build()
        );

        // 1. 등록
        PetCreateRequest createRequest = new PetCreateRequest();
        createRequest.setName("뽀삐");
        createRequest.setBreed("포메라니안");

        PetResponse created = petService.createPet(member.getId(), createRequest);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("뽀삐");

        // 2. 조회
        PetResponse found = petService.findById(created.getId());
        assertThat(found.getName()).isEqualTo("뽀삐");

        // 3. 수정
        PetUpdateRequest updateRequest = new PetUpdateRequest();
        updateRequest.setName("뽀삐2");

        PetResponse updated = petService.updatePet(member.getId(), created.getId(), updateRequest);
        assertThat(updated.getName()).isEqualTo("뽀삐2");

        // 4. 삭제
        petService.deletePet(member.getId(), created.getId());

        // 5. 삭제 확인
        assertThatThrownBy(() -> petService.findById(created.getId()))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
    }
}
```

### 비즈니스 시나리오 테스트

```java
@Nested
@DisplayName("산책 시나리오")
class WalkScenario {

    @Test
    @DisplayName("반려동물과 함께 산책을 시작하고 완료할 수 있다")
    void walkFlow() {
        // given - 회원과 반려동물 생성
        Member member = memberRepository.save(
            Member.builder().email("test@test.com").nickname("테스터").build()
        );
        Pet pet = petRepository.save(
            Pet.builder().memberId(member.getId()).name("뽀삐").build()
        );

        // when - 산책 시작
        WalkStartRequest startRequest = new WalkStartRequest();
        startRequest.setPetId(pet.getId());
        WalkResponse started = walkService.startWalk(member.getId(), startRequest);

        assertThat(started.getStatus()).isEqualTo(WalkStatus.IN_PROGRESS);

        // when - 산책 완료
        WalkCompleteRequest completeRequest = new WalkCompleteRequest();
        completeRequest.setDistance(1500);
        completeRequest.setDuration(30);

        WalkResponse completed = walkService.completeWalk(member.getId(), started.getId(), completeRequest);

        // then
        assertThat(completed.getStatus()).isEqualTo(WalkStatus.COMPLETED);
        assertThat(completed.getDistance()).isEqualTo(1500);
        assertThat(completed.getDuration()).isEqualTo(30);
    }
}
```

## 예외 시나리오 테스트

```java
@Nested
@DisplayName("예외 시나리오")
class ExceptionScenarios {

    @Test
    @DisplayName("다른 회원의 반려동물을 수정하면 예외가 발생한다")
    void updateOtherMembersPet_ThrowsException() {
        // given
        Member owner = memberRepository.save(
            Member.builder().email("owner@test.com").nickname("주인").build()
        );
        Member other = memberRepository.save(
            Member.builder().email("other@test.com").nickname("다른사람").build()
        );

        PetCreateRequest request = new PetCreateRequest();
        request.setName("뽀삐");
        PetResponse pet = petService.createPet(owner.getId(), request);

        PetUpdateRequest updateRequest = new PetUpdateRequest();
        updateRequest.setName("바꿀이름");

        // when & then
        assertThatThrownBy(() ->
            petService.updatePet(other.getId(), pet.getId(), updateRequest)
        )
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_PET_OWNER);
    }

    @Test
    @DisplayName("진행 중인 산책이 있으면 새 산책을 시작할 수 없다")
    void startWalk_WithExistingWalk_ThrowsException() {
        // given
        Member member = memberRepository.save(
            Member.builder().email("test@test.com").nickname("테스터").build()
        );
        Pet pet = petRepository.save(
            Pet.builder().memberId(member.getId()).name("뽀삐").build()
        );

        // 첫 번째 산책 시작
        WalkStartRequest request = new WalkStartRequest();
        request.setPetId(pet.getId());
        walkService.startWalk(member.getId(), request);

        // when & then - 두 번째 산책 시도
        assertThatThrownBy(() ->
            walkService.startWalk(member.getId(), request)
        )
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", WalkErrorCode.WALK_ALREADY_IN_PROGRESS);
    }
}
```

## 트랜잭션 테스트

```java
@Nested
@DisplayName("트랜잭션")
class TransactionTests {

    @Test
    @DisplayName("예외 발생 시 모든 변경사항이 롤백된다")
    void rollbackOnException() {
        // given
        Member member = memberRepository.save(
            Member.builder().email("test@test.com").nickname("테스터").build()
        );

        long initialCount = petRepository.count();

        // when - 일부 저장 후 예외 발생하는 서비스 호출
        try {
            petService.createMultiplePetsWithError(member.getId());
        } catch (Exception ignored) {
            // 예외 무시
        }

        // then - 롤백되어 카운트 변화 없음
        assertThat(petRepository.count()).isEqualTo(initialCount);
    }
}
```

## MockMvc를 함께 사용 (API 통합 테스트)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PetApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PetRepository petRepository;

    @Test
    @DisplayName("API를 통해 반려동물을 등록하고 조회한다")
    @WithMockUser(username = "1")  // memberId = 1
    void createAndFindViaApi() throws Exception {
        // given
        Member member = memberRepository.save(
            Member.builder().email("test@test.com").nickname("테스터").build()
        );

        PetCreateRequest request = new PetCreateRequest();
        request.setName("뽀삐");
        request.setBreed("포메라니안");

        // when - 등록
        String createResponse = mockMvc.perform(post("/api/pets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long petId = JsonPath.parse(createResponse).read("$.data.id", Long.class);

        // then - 조회
        mockMvc.perform(get("/api/pets/{petId}", petId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("뽀삐"))
            .andExpect(jsonPath("$.data.breed").value("포메라니안"));
    }
}
```

## 테스트 설정

### 테스트용 application.yml

```yaml
# src/test/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  sql:
    init:
      mode: never

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql: TRACE
```

### 테스트 프로파일 활성화

```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntegrationTest {
    // ...
}
```

## 주의사항

1. **@Transactional 필수**: 테스트 후 롤백을 위해 반드시 추가
2. **적절한 격리**: 테스트 간 데이터 간섭 주의
3. **성능 고려**: 통합 테스트는 느리므로 꼭 필요한 경우만 사용
4. **단위 테스트 우선**: 가능하면 단위 테스트로 커버하고, 통합 테스트는 흐름 검증용
