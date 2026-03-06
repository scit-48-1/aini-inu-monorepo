package scit.ainiinu.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.chat.dto.request.ChatMessageCreateRequest;
import scit.ainiinu.chat.dto.request.MessageReadRequest;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;
import scit.ainiinu.chat.dto.response.MessageReadResponse;
import scit.ainiinu.chat.entity.ChatMessageType;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.realtime.ChatRealtimeEventHandler;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.response.CursorResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatParticipantRepository chatParticipantRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRealtimeEventHandler chatRealtimeEventHandler;

    @InjectMocks
    private MessageService messageService;

    @Nested
    @DisplayName("createMessage")
    class CreateMessage {

        @Test
        @DisplayName("정상적으로 메시지를 생성하고 이벤트를 발행한다")
        void success() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE);
            ReflectionTestUtils.setField(room, "id", chatRoomId);

            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("안녕하세요");
            request.setMessageType("USER");
            request.setClientMessageId("msg-001");

            Message saved = Message.create(chatRoomId, memberId, "안녕하세요", ChatMessageType.USER, "msg-001");
            ReflectionTestUtils.setField(saved, "id", 100L);

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);
            given(messageRepository.save(any(Message.class))).willReturn(saved);

            // when
            ChatMessageResponse response = messageService.createMessage(memberId, chatRoomId, request);

            // then
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getContent()).isEqualTo("안녕하세요");
            then(chatRealtimeEventHandler).should().publishMessageCreated(eq(chatRoomId), any());
            then(chatRealtimeEventHandler).should().publishMessageDelivered(eq(chatRoomId), eq(100L), eq(memberId), any());
        }

        @Test
        @DisplayName("존재하지 않는 방이면 ROOM_NOT_FOUND 예외")
        void roomNotFound_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 999L;
            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("테스트");

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> messageService.createMessage(memberId, chatRoomId, request))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.ROOM_NOT_FOUND);
        }

        @Test
        @DisplayName("닫힌 방이면 ROOM_CLOSED 예외")
        void closedRoom_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.CLOSED);
            ReflectionTestUtils.setField(room, "id", chatRoomId);

            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("테스트");

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> messageService.createMessage(memberId, chatRoomId, request))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.ROOM_CLOSED);
        }

        @Test
        @DisplayName("참여자가 아니면 ROOM_ACCESS_DENIED 예외")
        void nonParticipant_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE);
            ReflectionTestUtils.setField(room, "id", chatRoomId);

            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("테스트");

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> messageService.createMessage(memberId, chatRoomId, request))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.ROOM_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("getMessages")
    class GetMessages {

        @Test
        @DisplayName("커서 페이지네이션으로 메시지를 조회한다")
        void success_withCursorPagination() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);

            Message msg1 = Message.create(chatRoomId, 2L, "메시지1", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(msg1, "id", 1L);
            Message msg2 = Message.create(chatRoomId, 2L, "메시지2", ChatMessageType.USER, "c2");
            ReflectionTestUtils.setField(msg2, "id", 2L);

            given(messageRepository.findByRoomIdWithCursor(chatRoomId, null, 51, "before"))
                    .willReturn(List.of(msg1, msg2));

            // when
            CursorResponse<ChatMessageResponse> response = messageService.getMessages(memberId, chatRoomId, null, null, null);

            // then
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.isHasMore()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("결과가 pageSize보다 많으면 hasMore=true, nextCursor 반환")
        void hasMore_returnsNextCursor() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            int size = 2;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);

            Message msg1 = Message.create(chatRoomId, 2L, "메시지1", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(msg1, "id", 1L);
            Message msg2 = Message.create(chatRoomId, 2L, "메시지2", ChatMessageType.USER, "c2");
            ReflectionTestUtils.setField(msg2, "id", 2L);
            Message msg3 = Message.create(chatRoomId, 2L, "메시지3", ChatMessageType.USER, "c3");
            ReflectionTestUtils.setField(msg3, "id", 3L);

            given(messageRepository.findByRoomIdWithCursor(chatRoomId, null, 3, "before"))
                    .willReturn(List.of(msg1, msg2, msg3));

            // when
            CursorResponse<ChatMessageResponse> response = messageService.getMessages(memberId, chatRoomId, null, size, null);

            // then
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.isHasMore()).isTrue();
            assertThat(response.getNextCursor()).isEqualTo("2");
        }
    }

    @Nested
    @DisplayName("markRead")
    class MarkRead {

        @Test
        @DisplayName("정상적으로 읽음 처리하고 이벤트를 발행한다")
        void success() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatParticipant participant = ChatParticipant.create(chatRoomId, memberId);
            ReflectionTestUtils.setField(participant, "id", 1L);

            Message message = Message.create(chatRoomId, 2L, "테스트", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(message, "id", 50L);

            MessageReadRequest request = new MessageReadRequest();
            request.setMessageId(50L);

            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(Optional.of(participant));
            given(messageRepository.findById(50L)).willReturn(Optional.of(message));

            // when
            MessageReadResponse response = messageService.markRead(memberId, chatRoomId, request);

            // then
            assertThat(response.getRoomId()).isEqualTo(chatRoomId);
            assertThat(response.getMemberId()).isEqualTo(memberId);
            then(chatRealtimeEventHandler).should().publishMessageRead(eq(chatRoomId), eq(50L), eq(memberId), any());
        }

        @Test
        @DisplayName("메시지가 다른 방의 것이면 MESSAGE_NOT_FOUND 예외")
        void messageFromDifferentRoom_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            ChatParticipant participant = ChatParticipant.create(chatRoomId, memberId);
            ReflectionTestUtils.setField(participant, "id", 1L);

            Message message = Message.create(999L, 2L, "다른방 메시지", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(message, "id", 50L);

            MessageReadRequest request = new MessageReadRequest();
            request.setMessageId(50L);

            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId))
                    .willReturn(Optional.of(participant));
            given(messageRepository.findById(50L)).willReturn(Optional.of(message));

            // when & then
            assertThatThrownBy(() -> messageService.markRead(memberId, chatRoomId, request))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.MESSAGE_NOT_FOUND);
        }
    }
}
