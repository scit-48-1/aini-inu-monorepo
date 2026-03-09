package scit.ainiinu.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.chat.dto.request.ChatRoomDirectCreateRequest;
import scit.ainiinu.chat.dto.request.WalkConfirmRequest;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.dto.response.WalkConfirmResponse;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.repository.ChatParticipantPetRepository;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.repository.PetRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ChatServiceUnitTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private ChatParticipantPetRepository chatParticipantPetRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private scit.ainiinu.notification.service.NotificationService notificationService;

    @Mock
    private scit.ainiinu.walk.repository.WalkThreadRepository walkThreadRepository;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @InjectMocks
    private WalkConfirmService walkConfirmService;

    @Nested
    @DisplayName("1:1 채팅방 멱등 생성")
    class DirectRoomIdempotency {

        @Test
        @DisplayName("기존 ACTIVE 방 + 양쪽 참여 중 → 기존 방 반환")
        void returnsExistingActiveRoom() {
            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            ChatParticipant me = ChatParticipant.create(10L, 1L);
            setParticipantId(me, 1L);
            ChatParticipant partner = ChatParticipant.create(10L, 2L);
            setParticipantId(partner, 2L);

            given(memberRepository.existsById(2L)).willReturn(true);
            given(chatRoomRepository.findDirectRoomsByParticipants(ChatRoomType.DIRECT, 1L, 2L, null))
                    .willReturn(List.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(10L, 1L))
                    .willReturn(Optional.of(me));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(10L, 2L))
                    .willReturn(Optional.of(partner));
            stubDetailResponse(10L, me, partner);

            // when
            ChatRoomDetailResponse response = chatRoomService.createDirectRoom(1L, request);

            // then
            assertThat(response.getChatRoomId()).isEqualTo(10L);
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
            then(chatRoomRepository).should().findDirectRoomsByParticipants(ChatRoomType.DIRECT, 1L, 2L, null);
        }

        @Test
        @DisplayName("기존 ACTIVE 방 + 한쪽 나감 → rejoin 후 기존 방 반환")
        void rejoinsLeftParticipant() {
            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);

            ChatParticipant me = ChatParticipant.create(10L, 1L);
            setParticipantId(me, 1L);
            ChatParticipant partner = ChatParticipant.create(10L, 2L);
            setParticipantId(partner, 2L);
            partner.leave(); // partner left

            given(memberRepository.existsById(2L)).willReturn(true);
            given(chatRoomRepository.findDirectRoomsByParticipants(ChatRoomType.DIRECT, 1L, 2L, null))
                    .willReturn(List.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(10L, 1L))
                    .willReturn(Optional.of(me));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(10L, 2L))
                    .willReturn(Optional.of(partner));
            stubDetailResponse(10L, me, partner);

            // when
            ChatRoomDetailResponse response = chatRoomService.createDirectRoom(1L, request);

            // then
            assertThat(response.getChatRoomId()).isEqualTo(10L);
            assertThat(partner.isLeft()).isFalse(); // partner rejoined
        }

        @Test
        @DisplayName("기존 CLOSED 방 → reopen + rejoin 후 기존 방 반환")
        void reopensClosedRoom() {
            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 10L);
            room.close(); // room is CLOSED

            ChatParticipant me = ChatParticipant.create(10L, 1L);
            setParticipantId(me, 1L);
            me.leave();
            ChatParticipant partner = ChatParticipant.create(10L, 2L);
            setParticipantId(partner, 2L);
            partner.leave();

            given(memberRepository.existsById(2L)).willReturn(true);
            given(chatRoomRepository.findDirectRoomsByParticipants(ChatRoomType.DIRECT, 1L, 2L, null))
                    .willReturn(List.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(10L, 1L))
                    .willReturn(Optional.of(me));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(10L, 2L))
                    .willReturn(Optional.of(partner));
            stubDetailResponse(10L, me, partner);

            // when
            ChatRoomDetailResponse response = chatRoomService.createDirectRoom(1L, request);

            // then
            assertThat(response.getChatRoomId()).isEqualTo(10L);
            assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE); // reopened
            assertThat(me.isLeft()).isFalse(); // rejoined
            assertThat(partner.isLeft()).isFalse(); // rejoined
        }

        @Test
        @DisplayName("방 없음 → 새 방 생성")
        void createsNewRoomWhenNoneExists() {
            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);

            ChatRoom newRoom = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(newRoom, 20L);

            ChatParticipant me = ChatParticipant.create(20L, 1L);
            setParticipantId(me, 1L);
            ChatParticipant partner = ChatParticipant.create(20L, 2L);
            setParticipantId(partner, 2L);

            given(memberRepository.existsById(2L)).willReturn(true);
            given(chatRoomRepository.findDirectRoomsByParticipants(ChatRoomType.DIRECT, 1L, 2L, null))
                    .willReturn(Collections.emptyList());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);
            given(chatParticipantRepository.save(any(ChatParticipant.class)))
                    .willReturn(me, partner);
            given(petRepository.findAllByMemberIdOrderByIsMainDesc(1L)).willReturn(List.of());
            given(petRepository.findAllByMemberIdOrderByIsMainDesc(2L)).willReturn(List.of());
            stubDetailResponse(20L, me, partner);

            // when
            ChatRoomDetailResponse response = chatRoomService.createDirectRoom(1L, request);

            // then
            assertThat(response.getChatRoomId()).isEqualTo(20L);
            then(chatRoomRepository).should().save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("DM 방이 이미 있어도 LOST_PET 요청 시 새 방 생성")
        void createsNewLostPetRoomEvenWhenDmExists() {
            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);
            request.setOrigin("LOST_PET");

            ChatRoom newRoom = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.LOST_PET, null);
            setRoomId(newRoom, 30L);

            ChatParticipant me = ChatParticipant.create(30L, 1L);
            setParticipantId(me, 1L);
            ChatParticipant partner = ChatParticipant.create(30L, 2L);
            setParticipantId(partner, 2L);

            given(memberRepository.existsById(2L)).willReturn(true);
            given(chatRoomRepository.findDirectRoomsByParticipants(ChatRoomType.DIRECT, 1L, 2L, ChatRoomOrigin.LOST_PET))
                    .willReturn(Collections.emptyList());
            given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(newRoom);
            given(chatParticipantRepository.save(any(ChatParticipant.class)))
                    .willReturn(me, partner);
            given(petRepository.findAllByMemberIdOrderByIsMainDesc(1L)).willReturn(List.of());
            given(petRepository.findAllByMemberIdOrderByIsMainDesc(2L)).willReturn(List.of());
            stubDetailResponse(30L, me, partner);

            // when
            ChatRoomDetailResponse response = chatRoomService.createDirectRoom(1L, request);

            // then
            assertThat(response.getChatRoomId()).isEqualTo(30L);
            then(chatRoomRepository).should().save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("같은 origin(LOST_PET) 방이 이미 있으면 재사용")
        void reusesExistingLostPetRoom() {
            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);
            request.setOrigin("LOST_PET");

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.LOST_PET, null);
            setRoomId(room, 40L);

            ChatParticipant me = ChatParticipant.create(40L, 1L);
            setParticipantId(me, 1L);
            ChatParticipant partner = ChatParticipant.create(40L, 2L);
            setParticipantId(partner, 2L);

            given(memberRepository.existsById(2L)).willReturn(true);
            given(chatRoomRepository.findDirectRoomsByParticipants(ChatRoomType.DIRECT, 1L, 2L, ChatRoomOrigin.LOST_PET))
                    .willReturn(List.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(40L, 1L))
                    .willReturn(Optional.of(me));
            given(chatParticipantRepository.findByChatRoomIdAndMemberId(40L, 2L))
                    .willReturn(Optional.of(partner));
            stubDetailResponse(40L, me, partner);

            // when
            ChatRoomDetailResponse response = chatRoomService.createDirectRoom(1L, request);

            // then
            assertThat(response.getChatRoomId()).isEqualTo(40L);
            then(chatRoomRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("산책확인 상태")
    class WalkConfirm {

        @Test
        @DisplayName("CONFIRM 요청 시 본인 상태를 CONFIRMED로 갱신한다")
        void confirmAction_updatesState() {
            // given
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            setRoomId(room, 100L);

            ChatParticipant me = ChatParticipant.create(100L, 1L);
            setParticipantId(me, 1L);
            ChatParticipant other = ChatParticipant.create(100L, 2L);
            setParticipantId(other, 2L);

            WalkConfirmRequest request = new WalkConfirmRequest();
            request.setAction("CONFIRM");

            given(chatRoomRepository.findByIdForUpdate(100L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(100L, 1L)).willReturn(Optional.of(me));
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(100L)).willReturn(List.of(me, other));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            // when
            WalkConfirmResponse response = walkConfirmService.updateWalkConfirm(1L, 100L, request);

            // then
            assertThat(response.getMyState()).isEqualTo("CONFIRMED");
            assertThat(response.getConfirmedMemberIds()).containsExactly(1L);

            // 아직 전원 확정이 아니므로 확인자 외 다른 참여자에게만 알림이 발행되어야 한다
            then(notificationService).should().createAndPublish(
                    eq(2L),
                    eq(scit.ainiinu.notification.entity.NotificationType.WALK_CONFIRM),
                    anyString(), anyString(), eq(100L), eq("CHAT_ROOM")
            );
        }

        @Test
        @DisplayName("지원하지 않는 action이면 예외를 던진다")
        void invalidAction_throwsException() {
            // given
            WalkConfirmRequest request = new WalkConfirmRequest();
            request.setAction("BAD_ACTION");

            // when & then
            assertThatThrownBy(() -> walkConfirmService.updateWalkConfirm(1L, 100L, request))
                    .isInstanceOf(ChatException.class);
        }
    }

    private void stubDetailResponse(Long roomId, ChatParticipant... participants) {
        given(chatParticipantRepository.findAllByChatRoomId(roomId)).willReturn(List.of(participants));
        List<Long> participantIds = java.util.Arrays.stream(participants).map(ChatParticipant::getId).toList();
        given(chatParticipantPetRepository.findAllByChatParticipantIdIn(participantIds)).willReturn(List.of());
        given(messageRepository.findTopByChatRoomIdOrderByIdDesc(roomId)).willReturn(Optional.empty());
    }

    private void setRoomId(ChatRoom room, Long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(room, "id", id);
    }

    private void setParticipantId(ChatParticipant participant, Long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(participant, "id", id);
    }
}
