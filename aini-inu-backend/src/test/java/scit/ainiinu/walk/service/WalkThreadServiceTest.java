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
import scit.ainiinu.walk.repository.WalkThreadApplicationPetRepository;
import scit.ainiinu.walk.repository.WalkThreadApplicationRepository;
import scit.ainiinu.walk.repository.WalkThreadFilterRepository;
import scit.ainiinu.walk.repository.WalkThreadPetRepository;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import scit.ainiinu.walk.entity.WalkThreadApplicationPet;
import scit.ainiinu.walk.entity.WalkThreadApplicationStatus;
import scit.ainiinu.notification.entity.NotificationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
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
    private WalkThreadApplicationPetRepository walkThreadApplicationPetRepository;

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

    @Mock
    private scit.ainiinu.notification.service.NotificationService notificationService;

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

            // 멱등 재시도에는 알림을 발행하지 않아야 한다
            then(notificationService).should(never()).createAndPublish(
                    anyLong(), any(), anyString(), anyString(), anyLong(), anyString()
            );
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

            WalkThreadApplication savedApplication = WalkThreadApplication.joined(1L, 2L, 101L);
            ReflectionTestUtils.setField(savedApplication, "id", 1L);
            given(walkThreadApplicationRepository.save(any(WalkThreadApplication.class))).willReturn(savedApplication);

            // 신청자 닉네임 조회용 memberRepository stub
            Member applicant = Member.builder()
                    .email("applicant@test.com")
                    .nickname("신청자")
                    .memberType(MemberType.PET_OWNER)
                    .build();
            ReflectionTestUtils.setField(applicant, "id", 2L);
            given(memberRepository.findById(2L)).willReturn(Optional.of(applicant));

            // when
            ThreadApplyResponse response = walkThreadService.applyThread(2L, 1L, request);

            // then
            assertThat(response.isIdempotentReplay()).isFalse();
            assertThat(response.getChatRoomId()).isEqualTo(101L);
            assertThat(response.getApplicationStatus()).isEqualTo("JOINED");
            then(chatRoomRepository).should().save(any(ChatRoom.class));
            then(chatParticipantRepository).should(times(2)).save(any());

            // 스레드 작성자(10L)에게 WALK_APPLICATION 알림이 발행되어야 한다
            then(notificationService).should().createAndPublish(
                    eq(10L),
                    eq(NotificationType.WALK_APPLICATION),
                    eq("산책 참여신청"),
                    eq("신청자님이 산책에 참여를 신청했습니다."),
                    eq(1L),
                    eq("WALK_THREAD")
            );
        }
    }

    @Nested
    @DisplayName("신청 시 반려견 저장")
    class ApplyThreadPets {

        @Test
        @DisplayName("신규 신청 시 WalkThreadApplicationPet이 저장된다")
        void apply_savesApplicationPets() {
            // given
            WalkThread thread = buildFutureThread(1L, 10L);

            ChatRoom savedRoom = ChatRoom.create(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            ReflectionTestUtils.setField(savedRoom, "id", 101L);

            ThreadApplyRequest request = new ThreadApplyRequest();
            request.setPetIds(List.of(201L, 202L));

            WalkThreadApplication savedApplication = WalkThreadApplication.joined(1L, 2L, 101L);
            ReflectionTestUtils.setField(savedApplication, "id", 55L);

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberId(1L, 2L)).willReturn(Optional.empty());
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(0L);
            given(chatRoomRepository.findFirstByThreadIdAndChatTypeAndStatusOrderByIdAsc(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 10L)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 2L)).willReturn(Optional.empty());
            given(walkThreadApplicationRepository.save(any(WalkThreadApplication.class))).willReturn(savedApplication);
            given(memberRepository.findById(2L)).willReturn(Optional.empty());

            // when
            walkThreadService.applyThread(2L, 1L, request);

            // then — 알림이 스레드 작성자에게 발행된다
            then(notificationService).should().createAndPublish(
                    eq(10L), eq(NotificationType.WALK_APPLICATION),
                    anyString(), anyString(), eq(1L), eq("WALK_THREAD")
            );

            // then — 2 pets saved for application ID 55
            ArgumentCaptor<WalkThreadApplicationPet> captor = ArgumentCaptor.forClass(WalkThreadApplicationPet.class);
            then(walkThreadApplicationPetRepository).should(times(2)).save(captor.capture());

            List<WalkThreadApplicationPet> saved = captor.getAllValues();
            assertThat(saved).extracting(WalkThreadApplicationPet::getApplicationId).containsOnly(55L);
            assertThat(saved).extracting(WalkThreadApplicationPet::getPetId).containsExactly(201L, 202L);
        }

        @Test
        @DisplayName("petIds가 빈 리스트이면 ApplicationPet을 저장하지 않는다")
        void apply_emptyPetIds_skipsApplicationPets() {
            // given
            WalkThread thread = buildFutureThread(1L, 10L);

            ChatRoom savedRoom = ChatRoom.create(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            ReflectionTestUtils.setField(savedRoom, "id", 101L);

            ThreadApplyRequest request = new ThreadApplyRequest();
            request.setPetIds(List.of());

            WalkThreadApplication savedApplication = WalkThreadApplication.joined(1L, 2L, 101L);
            ReflectionTestUtils.setField(savedApplication, "id", 55L);

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberId(1L, 2L)).willReturn(Optional.empty());
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(0L);
            given(chatRoomRepository.findFirstByThreadIdAndChatTypeAndStatusOrderByIdAsc(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 10L)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 2L)).willReturn(Optional.empty());
            given(walkThreadApplicationRepository.save(any(WalkThreadApplication.class))).willReturn(savedApplication);
            given(memberRepository.findById(2L)).willReturn(Optional.empty());

            // when
            walkThreadService.applyThread(2L, 1L, request);

            // then — 알림이 스레드 작성자에게 발행된다
            then(notificationService).should().createAndPublish(
                    eq(10L), eq(NotificationType.WALK_APPLICATION),
                    anyString(), anyString(), eq(1L), eq("WALK_THREAD")
            );

            // then
            then(walkThreadApplicationPetRepository).should(never()).save(any(WalkThreadApplicationPet.class));
        }

        @Test
        @DisplayName("재참여(rejoin) 시 기존 반려견을 삭제하고 새 반려견을 저장한다")
        void apply_rejoin_deletesOldAndSavesNewPets() {
            // given
            WalkThread thread = buildFutureThread(1L, 10L);

            ChatRoom savedRoom = ChatRoom.create(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            ReflectionTestUtils.setField(savedRoom, "id", 101L);

            ThreadApplyRequest request = new ThreadApplyRequest();
            request.setPetIds(List.of(301L));

            WalkThreadApplication existingCanceled = WalkThreadApplication.canceled(1L, 2L);
            ReflectionTestUtils.setField(existingCanceled, "id", 77L);

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberId(1L, 2L)).willReturn(Optional.of(existingCanceled));
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(0L);
            given(chatRoomRepository.findFirstByThreadIdAndChatTypeAndStatusOrderByIdAsc(1L, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 10L)).willReturn(Optional.empty());
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(101L, 2L)).willReturn(Optional.empty());
            given(memberRepository.findById(2L)).willReturn(Optional.empty());

            // when
            walkThreadService.applyThread(2L, 1L, request);

            // then — 알림이 스레드 작성자에게 발행된다
            then(notificationService).should().createAndPublish(
                    eq(10L), eq(NotificationType.WALK_APPLICATION),
                    anyString(), anyString(), eq(1L), eq("WALK_THREAD")
            );

            // then — old pets deleted, new pet saved
            then(walkThreadApplicationPetRepository).should().deleteAllByApplicationId(77L);

            ArgumentCaptor<WalkThreadApplicationPet> captor = ArgumentCaptor.forClass(WalkThreadApplicationPet.class);
            then(walkThreadApplicationPetRepository).should().save(captor.capture());
            assertThat(captor.getValue().getApplicationId()).isEqualTo(77L);
            assertThat(captor.getValue().getPetId()).isEqualTo(301L);
        }
    }

    @Nested
    @DisplayName("신청 취소 시 반려견 삭제")
    class CancelApplyThreadPets {

        @Test
        @DisplayName("취소 시 해당 신청의 반려견이 모두 삭제된다")
        void cancel_deletesApplicationPets() {
            // given
            WalkThreadApplication application = WalkThreadApplication.joined(1L, 2L, 9001L);
            ReflectionTestUtils.setField(application, "id", 42L);

            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, 2L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.of(application));

            // when
            walkThreadService.cancelApplyThread(2L, 1L);

            // then
            assertThat(application.getStatus()).isEqualTo(WalkThreadApplicationStatus.CANCELED);
            then(walkThreadApplicationRepository).should().flush();
            then(walkThreadApplicationPetRepository).should().deleteAllByApplicationId(42L);
        }
    }

    @Nested
    @DisplayName("스레드 상세 조회 - PetSummary")
    class GetThreadPetSummary {

        @Test
        @DisplayName("작성자 + 참여자 반려견이 모두 pets에 포함된다")
        void getThread_includesAuthorAndApplicantPets() {
            // given
            WalkThread thread = buildFutureThread(1L, 10L);

            // Author pet
            WalkThreadPet authorPet = WalkThreadPet.of(1L, 100L);

            // Joined application with pet
            WalkThreadApplication joinedApp = WalkThreadApplication.joined(1L, 20L, 5001L);
            ReflectionTestUtils.setField(joinedApp, "id", 50L);

            WalkThreadApplicationPet appPet = WalkThreadApplicationPet.of(50L, 200L);

            // Pet entities
            Pet pet100 = Pet.builder()
                    .memberId(10L).name("몽이").age(3)
                    .gender(scit.ainiinu.pet.entity.enums.PetGender.MALE)
                    .size(scit.ainiinu.pet.entity.enums.PetSize.MEDIUM)
                    .isNeutered(true).isMain(true).build();
            ReflectionTestUtils.setField(pet100, "id", 100L);
            ReflectionTestUtils.setField(pet100, "photoUrl", "https://cdn.example.com/mongi.jpg");
            ReflectionTestUtils.setField(pet100, "mbti", "ENFP");

            Pet pet200 = Pet.builder()
                    .memberId(20L).name("코코").age(2)
                    .gender(scit.ainiinu.pet.entity.enums.PetGender.FEMALE)
                    .size(scit.ainiinu.pet.entity.enums.PetSize.SMALL)
                    .isNeutered(false).isMain(true).build();
            ReflectionTestUtils.setField(pet200, "id", 200L);

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(1L);
            given(walkThreadPetRepository.findAllByThreadId(1L)).willReturn(List.of(authorPet));
            given(walkThreadApplicationRepository.findAllByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(joinedApp));
            given(walkThreadApplicationPetRepository.findAllByApplicationIdIn(List.of(50L)))
                    .willReturn(List.of(appPet));
            given(petRepository.findAllById(any())).willReturn(List.of(pet100, pet200));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, 10L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

            // when
            ThreadResponse response = walkThreadService.getThread(10L, 1L);

            // then
            assertThat(response.getPets()).hasSize(2);
            assertThat(response.getPets()).extracting(ThreadResponse.PetSummary::getId).containsExactlyInAnyOrder(100L, 200L);

            ThreadResponse.PetSummary mongi = response.getPets().stream()
                    .filter(p -> p.getId().equals(100L)).findFirst().orElseThrow();
            assertThat(mongi.getName()).isEqualTo("몽이");
            assertThat(mongi.getPhotoUrl()).isEqualTo("https://cdn.example.com/mongi.jpg");
            assertThat(mongi.getAge()).isEqualTo(3);
            assertThat(mongi.getGender()).isEqualTo("MALE");
            assertThat(mongi.getSize()).isEqualTo("MEDIUM");
            assertThat(mongi.getMbti()).isEqualTo("ENFP");
            assertThat(mongi.getIsNeutered()).isTrue();

            ThreadResponse.PetSummary coco = response.getPets().stream()
                    .filter(p -> p.getId().equals(200L)).findFirst().orElseThrow();
            assertThat(coco.getName()).isEqualTo("코코");
            assertThat(coco.getGender()).isEqualTo("FEMALE");
            assertThat(coco.getSize()).isEqualTo("SMALL");
            assertThat(coco.getIsNeutered()).isFalse();
        }

        @Test
        @DisplayName("참여 신청이 없으면 작성자 반려견만 포함된다")
        void getThread_noApplicants_onlyAuthorPets() {
            // given
            WalkThread thread = buildFutureThread(1L, 10L);
            WalkThreadPet authorPet = WalkThreadPet.of(1L, 100L);

            Pet pet100 = Pet.builder()
                    .memberId(10L).name("몽이").age(3)
                    .gender(scit.ainiinu.pet.entity.enums.PetGender.MALE)
                    .size(scit.ainiinu.pet.entity.enums.PetSize.MEDIUM)
                    .isNeutered(true).isMain(true).build();
            ReflectionTestUtils.setField(pet100, "id", 100L);

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(0L);
            given(walkThreadPetRepository.findAllByThreadId(1L)).willReturn(List.of(authorPet));
            given(walkThreadApplicationRepository.findAllByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(petRepository.findAllById(any())).willReturn(List.of(pet100));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, 10L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

            // when
            ThreadResponse response = walkThreadService.getThread(10L, 1L);

            // then
            assertThat(response.getPets()).hasSize(1);
            assertThat(response.getPets().get(0).getId()).isEqualTo(100L);
            assertThat(response.getPets().get(0).getName()).isEqualTo("몽이");

            // petIds는 작성자 것만
            assertThat(response.getPetIds()).containsExactly(100L);
        }

        @Test
        @DisplayName("작성자와 참여자가 같은 반려견을 등록해도 중복 없이 포함된다")
        void getThread_duplicatePetIds_deduped() {
            // given
            WalkThread thread = buildFutureThread(1L, 10L);
            WalkThreadPet authorPet = WalkThreadPet.of(1L, 100L);

            WalkThreadApplication joinedApp = WalkThreadApplication.joined(1L, 20L, 5001L);
            ReflectionTestUtils.setField(joinedApp, "id", 50L);

            // Same pet ID 100 as author
            WalkThreadApplicationPet appPet = WalkThreadApplicationPet.of(50L, 100L);

            Pet pet100 = Pet.builder()
                    .memberId(10L).name("몽이").age(3)
                    .gender(scit.ainiinu.pet.entity.enums.PetGender.MALE)
                    .size(scit.ainiinu.pet.entity.enums.PetSize.MEDIUM)
                    .isNeutered(true).isMain(true).build();
            ReflectionTestUtils.setField(pet100, "id", 100L);

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(1L);
            given(walkThreadPetRepository.findAllByThreadId(1L)).willReturn(List.of(authorPet));
            given(walkThreadApplicationRepository.findAllByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of(joinedApp));
            given(walkThreadApplicationPetRepository.findAllByApplicationIdIn(List.of(50L)))
                    .willReturn(List.of(appPet));
            // LinkedHashSet deduplication → only [100L] queried
            given(petRepository.findAllById(any())).willReturn(List.of(pet100));
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, 10L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

            // when
            ThreadResponse response = walkThreadService.getThread(10L, 1L);

            // then — deduped to 1 pet
            assertThat(response.getPets()).hasSize(1);
            assertThat(response.getPets().get(0).getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("반려견이 없는 스레드는 빈 pets 리스트를 반환한다")
        void getThread_noPets_emptyList() {
            // given
            WalkThread thread = buildFutureThread(1L, 10L);

            given(walkThreadRepository.findByIdAndStatusNot(1L, WalkThreadStatus.DELETED)).willReturn(Optional.of(thread));
            given(walkThreadApplicationRepository.countByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED)).willReturn(0L);
            given(walkThreadPetRepository.findAllByThreadId(1L)).willReturn(List.of());
            given(walkThreadApplicationRepository.findAllByThreadIdAndStatus(1L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(List.of());
            given(walkThreadApplicationRepository.findByThreadIdAndMemberIdAndStatus(1L, 10L, WalkThreadApplicationStatus.JOINED))
                    .willReturn(Optional.empty());

            // when
            ThreadResponse response = walkThreadService.getThread(10L, 1L);

            // then
            assertThat(response.getPets()).isEmpty();
            then(petRepository).should(never()).findAllById(any());
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

    private WalkThread buildFutureThread(Long id, Long authorId) {
        WalkThread thread = WalkThread.builder()
                .authorId(authorId)
                .title("산책 모집")
                .description("설명")
                .walkDate(LocalDate.now().plusDays(1))
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .chatType(WalkChatType.GROUP)
                .maxParticipants(5)
                .allowNonPetOwner(true)
                .isVisibleAlways(true)
                .placeName("서울숲")
                .latitude(BigDecimal.valueOf(37.54))
                .longitude(BigDecimal.valueOf(127.04))
                .address("성동구")
                .status(WalkThreadStatus.RECRUITING)
                .build();
        ReflectionTestUtils.setField(thread, "id", id);
        return thread;
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
