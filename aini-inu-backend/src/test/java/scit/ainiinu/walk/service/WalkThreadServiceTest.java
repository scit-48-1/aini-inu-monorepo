package scit.ainiinu.walk.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.member.entity.Member;
import scit.ainiinu.member.entity.enums.MemberType;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.walk.dto.request.ThreadApplyRequest;
import scit.ainiinu.walk.dto.request.ThreadCreateRequest;
import scit.ainiinu.walk.dto.response.ThreadApplyResponse;
import scit.ainiinu.walk.dto.response.ThreadMapResponse;
import scit.ainiinu.walk.dto.response.ThreadResponse;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkThreadApplication;
import scit.ainiinu.walk.entity.WalkThreadPet;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.exception.ThreadErrorCode;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadFilterRepository;
import scit.ainiinu.walk.repository.WalkThreadPetRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class WalkThreadServiceTest {

    @Mock
    private WalkThreadRepository walkThreadRepository;

    @Mock
    private WalkThreadPetRepository walkThreadPetRepository;

    @Mock
    private WalkThreadFilterRepository walkThreadFilterRepository;

    @Mock
    private WalkThreadApplicationRepository walkThreadApplicationRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WalkThreadService walkThreadService;

    @Nested
    @DisplayName("스레드 생성")
    class CreateThread {

        @Test
        @DisplayName("비애견인은 스레드를 생성할 수 없다")
        void create_nonPetOwner_fail() {
            // given
            Member member = Member.builder()
                    .email("non@pet.com")
                    .nickname("nonpet")
                    .memberType(MemberType.NON_PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(member, "id", 1L);

            ThreadCreateRequest request = createRequest();
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> walkThreadService.createThread(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ThreadErrorCode.NON_PET_OWNER_CREATE_FORBIDDEN);
        }

        @Test
        @DisplayName("유효한 요청이면 스레드를 생성한다")
        void create_success() {
            // given
            Member member = Member.builder()
                    .email("pet@owner.com")
                    .nickname("petowner")
                    .memberType(MemberType.PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(member, "id", 1L);

            ThreadCreateRequest request = createRequest();
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));
            given(walkThreadRepository.findAllByAuthorIdAndStatus(1L, WalkThreadStatus.RECRUITING)).willReturn(List.of());
            given(walkThreadRepository.save(any(WalkThread.class))).willAnswer(invocation -> {
                WalkThread thread = invocation.getArgument(0);
                ReflectionTestUtils.setField(thread, "id", 101L);
                return thread;
            });

            // when
            ThreadResponse response = walkThreadService.createThread(1L, request);

            // then
            assertThat(response.getId()).isEqualTo(101L);
            assertThat(response.getTitle()).isEqualTo("한강 산책 모집");

            ArgumentCaptor<WalkThread> captor = ArgumentCaptor.forClass(WalkThread.class);
            then(walkThreadRepository).should().save(captor.capture());
            assertThat(captor.getValue().getAuthorId()).isEqualTo(1L);

            // 이벤트 발행 검증
            org.mockito.ArgumentCaptor<scit.ainiinu.common.event.ContentCreatedEvent> eventCaptor =
                    org.mockito.ArgumentCaptor.forClass(scit.ainiinu.common.event.ContentCreatedEvent.class);
            then(eventPublisher).should().publishEvent(eventCaptor.capture());
            scit.ainiinu.common.event.ContentCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getMemberId()).isEqualTo(1L);
            assertThat(publishedEvent.getReferenceId()).isEqualTo(101L);
            assertThat(publishedEvent.getEventType()).isEqualTo(scit.ainiinu.common.event.TimelineEventType.WALK_THREAD_CREATED);
        }
    }

    @Nested
    @DisplayName("스레드 신청")
    class ApplyThread {

        @Test
        @DisplayName("중복 신청은 멱등 성공으로 반환한다")
        void apply_idempotent_success() {
            // given
            WalkThread thread = WalkThread.builder()
                    .authorId(10L)
                    .title("한강")
                    .description("설명")
                    .walkDate(LocalDate.now().plusDays(1))
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .chatType(scit.ainiinu.walk.entity.WalkChatType.GROUP)
                    .maxParticipants(5)
                    .allowNonPetOwner(true)
                    .isVisibleAlways(true)
                    .placeName("서울숲")
                    .latitude(java.math.BigDecimal.valueOf(37.54))
                    .longitude(java.math.BigDecimal.valueOf(127.04))
                    .address("성동구")
                    .status(WalkThreadStatus.RECRUITING)
                    .build();
            ReflectionTestUtils.setField(thread, "id", 1L);

            WalkThreadApplication existing = WalkThreadApplication.joined(1L, 2L, 9001L);

            ThreadApplyRequest request = new ThreadApplyRequest();
            request.setPetIds(List.of(1L));

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberId(1L, 2L)).willReturn(Optional.of(existing));

            // when
            ThreadApplyResponse response = walkThreadService.applyThread(2L, 1L, request);

            // then
            assertThat(response.isIdempotentReplay()).isTrue();
            assertThat(response.getChatRoomId()).isEqualTo(9001L);
            assertThat(response.getApplicationStatus()).isEqualTo("JOINED");
        }

        @Test
        @DisplayName("신규 신청은 실제 채팅방을 생성하고 참여자를 연결한다")
        void apply_createsRealChatRoom_success() {
            // given
            WalkThread thread = WalkThread.builder()
                    .authorId(10L)
                    .title("한강")
                    .description("설명")
                    .walkDate(LocalDate.now().plusDays(1))
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .chatType(scit.ainiinu.walk.entity.WalkChatType.GROUP)
                    .maxParticipants(5)
                    .allowNonPetOwner(true)
                    .isVisibleAlways(true)
                    .placeName("서울숲")
                    .latitude(java.math.BigDecimal.valueOf(37.54))
                    .longitude(java.math.BigDecimal.valueOf(127.04))
                    .address("성동구")
                    .status(WalkThreadStatus.RECRUITING)
                    .build();
            ReflectionTestUtils.setField(thread, "id", 1L);

            ChatRoom savedRoom = ChatRoom.create(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            ReflectionTestUtils.setField(savedRoom, "id", 101L);

            ThreadApplyRequest request = new ThreadApplyRequest();
            request.setPetIds(List.of(1L));

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberId(1L, 2L)).willReturn(Optional.empty());
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, scit.ainiinu.walk.entity.WalkThreadApplicationStatus.JOINED))
                    .willReturn(0L);
            given(chatRoomRepository.findFirstByThreadIdAndChatTypeAndStatusOrderByIdAsc(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 10L)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 2L)).willReturn(Optional.empty());

            // when
            ThreadApplyResponse response = walkThreadService.applyThread(2L, 1L, request);

            // then
            assertThat(response.isIdempotentReplay()).isFalse();
            assertThat(response.getChatRoomId()).isEqualTo(101L);
            assertThat(response.getApplicationStatus()).isEqualTo("JOINED");
            then(chatRoomRepository).should().save(any(ChatRoom.class));
            then(chatParticipantRepository).should(times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("지도 스레드 조회 (getMapThreads)")
    class GetMapThreads {

        @Test
        @DisplayName("성공: 스레드의 첫 번째 강아지 photoUrl이 petImageUrl에 포함된다")
        void getMapThreads_returnsPetImageUrl() {
            // given
            WalkThread thread = createRecruitingThread(1L, "서울숲 산책", 37.54, 127.04);

            given(walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(anyList(), any()))
                    .willReturn(List.of());

            WalkThreadPet threadPet = WalkThreadPet.of(1L, 100L);
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of(threadPet));

            Pet pet = Pet.builder().memberId(1L).build();
            ReflectionTestUtils.setField(pet, "id", 100L);
            ReflectionTestUtils.setField(pet, "photoUrl", "https://cdn.example.com/dog.jpg");
            given(petRepository.findAllById(List.of(100L)))
                    .willReturn(List.of(pet));

            // when
            List<ThreadMapResponse> results = walkThreadService.getMapThreads(
                    1L, 37.54, 127.04, 5.0, null, null);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPetImageUrl()).isEqualTo("https://cdn.example.com/dog.jpg");
        }

        @Test
        @DisplayName("성공: 강아지가 등록되지 않은 스레드는 petImageUrl이 null이다")
        void getMapThreads_noPet_returnsNullPetImageUrl() {
            // given
            WalkThread thread = createRecruitingThread(1L, "한강 산책", 37.52, 126.93);

            given(walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(anyList(), any()))
                    .willReturn(List.of());
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of());

            // when
            List<ThreadMapResponse> results = walkThreadService.getMapThreads(
                    1L, 37.52, 126.93, 5.0, null, null);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPetImageUrl()).isNull();
        }

        @Test
        @DisplayName("성공: 여러 강아지가 있으면 첫 번째 강아지의 photoUrl을 사용한다")
        void getMapThreads_multiplePets_usesFirstPetImage() {
            // given
            WalkThread thread = createRecruitingThread(1L, "올림픽공원 산책", 37.52, 127.12);

            given(walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(anyList(), any()))
                    .willReturn(List.of());

            WalkThreadPet firstPet = WalkThreadPet.of(1L, 100L);
            WalkThreadPet secondPet = WalkThreadPet.of(1L, 200L);
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of(firstPet, secondPet));

            Pet pet1 = Pet.builder().memberId(1L).build();
            ReflectionTestUtils.setField(pet1, "id", 100L);
            ReflectionTestUtils.setField(pet1, "photoUrl", "https://cdn.example.com/first.jpg");

            Pet pet2 = Pet.builder().memberId(1L).build();
            ReflectionTestUtils.setField(pet2, "id", 200L);
            ReflectionTestUtils.setField(pet2, "photoUrl", "https://cdn.example.com/second.jpg");

            given(petRepository.findAllById(anyList()))
                    .willReturn(List.of(pet1, pet2));

            // when
            List<ThreadMapResponse> results = walkThreadService.getMapThreads(
                    1L, 37.52, 127.12, 5.0, null, null);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPetImageUrl()).isEqualTo("https://cdn.example.com/first.jpg");
        }

        @Test
        @DisplayName("성공: 강아지의 photoUrl이 null이면 petImageUrl도 null이다")
        void getMapThreads_petWithNoPhoto_returnsNullPetImageUrl() {
            // given
            WalkThread thread = createRecruitingThread(1L, "양재천 산책", 37.47, 127.04);

            given(walkThreadRepository.findByStatus(WalkThreadStatus.RECRUITING))
                    .willReturn(List.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdInAndStatus(anyList(), any()))
                    .willReturn(List.of());

            WalkThreadPet threadPet = WalkThreadPet.of(1L, 100L);
            given(walkThreadPetRepository.findAllByThreadIdIn(List.of(1L)))
                    .willReturn(List.of(threadPet));

            Pet pet = Pet.builder().memberId(1L).build();
            ReflectionTestUtils.setField(pet, "id", 100L);
            given(petRepository.findAllById(List.of(100L)))
                    .willReturn(List.of(pet));

            // when
            List<ThreadMapResponse> results = walkThreadService.getMapThreads(
                    1L, 37.47, 127.04, 5.0, null, null);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getPetImageUrl()).isNull();
        }

        private WalkThread createRecruitingThread(Long id, String title, double lat, double lng) {
            WalkThread thread = WalkThread.builder()
                    .authorId(1L)
                    .title(title)
                    .description("테스트 설명")
                    .walkDate(LocalDate.now().plusDays(1))
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .chatType(WalkChatType.GROUP)
                    .maxParticipants(5)
                    .allowNonPetOwner(true)
                    .isVisibleAlways(true)
                    .placeName(title)
                    .latitude(BigDecimal.valueOf(lat))
                    .longitude(BigDecimal.valueOf(lng))
                    .address("테스트 주소")
                    .status(WalkThreadStatus.RECRUITING)
                    .build();
            ReflectionTestUtils.setField(thread, "id", id);
            return thread;
        }
    }

    private ThreadCreateRequest createRequest() {
        ThreadCreateRequest request = new ThreadCreateRequest();
        request.setTitle("한강 산책 모집");
        request.setDescription("저녁 산책 함께해요");
        request.setWalkDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        request.setChatType("GROUP");
        request.setMaxParticipants(5);
        request.setAllowNonPetOwner(true);
        request.setIsVisibleAlways(true);
        ThreadCreateRequest.LocationRequest location = new ThreadCreateRequest.LocationRequest();
        location.setPlaceName("서울숲");
        location.setLatitude(37.54);
        location.setLongitude(127.04);
        location.setAddress("성동구");
        request.setLocation(location);
        request.setPetIds(List.of(1L));
        return request;
    }
}
