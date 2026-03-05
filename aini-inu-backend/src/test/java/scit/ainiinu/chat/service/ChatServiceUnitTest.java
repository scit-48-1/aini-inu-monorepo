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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ChatRoomService chatRoomService;

    @InjectMocks
    private WalkConfirmService walkConfirmService;

    @Nested
    @DisplayName("1:1 채팅방 멱등 생성")
    class DirectRoomIdempotency {

        @Test
        @DisplayName("같은 참여자 direct 방이 있으면 기존 방을 반환한다")
        void returnsExistingDirectRoom() {
            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);

            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE);
            setRoomId(room, 10L);

            given(memberRepository.existsById(2L)).willReturn(true);
            given(chatRoomRepository.findByTypeAndParticipants(ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, 1L, 2L))
                    .willReturn(Optional.of(room));
            given(chatParticipantRepository.findAllByChatRoomId(10L)).willReturn(List.of());

            // when
            ChatRoomDetailResponse response = chatRoomService.createDirectRoom(1L, request);

            // then
            assertThat(response.getChatRoomId()).isEqualTo(10L);
            then(chatRoomRepository).should().findByTypeAndParticipants(ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, 1L, 2L);
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
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE);
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

            // when
            WalkConfirmResponse response = walkConfirmService.updateWalkConfirm(1L, 100L, request);

            // then
            assertThat(response.getMyState()).isEqualTo("CONFIRMED");
            assertThat(response.getConfirmedMemberIds()).containsExactly(1L);
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

    private void setRoomId(ChatRoom room, Long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(room, "id", id);
    }

    private void setParticipantId(ChatParticipant participant, Long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(participant, "id", id);
    }
}
