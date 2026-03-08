package scit.ainiinu.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.chat.dto.request.ChatMessageCreateRequest;
import scit.ainiinu.chat.dto.request.MessageReadRequest;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;
import scit.ainiinu.chat.dto.response.MessageReadResponse;
import scit.ainiinu.chat.entity.ChatMessageType;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.realtime.ChatRealtimeEventHandler;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.event.NotificationEvent;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

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

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
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
            then(chatRoomRepository).should().updateLastMessageAt(eq(chatRoomId), eq(saved.getSentAt()));
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

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.CLOSED, ChatRoomOrigin.DM, null);
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

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
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

        @Test
        @DisplayName("메시지 생성 시 발신자를 제외한 참가자에게 NotificationEvent가 발행된다")
        void publishesNotificationToOtherParticipants() {
            // given
            Long senderId = 1L;
            Long recipientId = 2L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", chatRoomId);

            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("안녕하세요");
            request.setMessageType("USER");
            request.setClientMessageId("msg-001");

            Message saved = Message.create(chatRoomId, senderId, "안녕하세요", ChatMessageType.USER, "msg-001");
            ReflectionTestUtils.setField(saved, "id", 100L);

            ChatParticipant senderParticipant = ChatParticipant.create(chatRoomId, senderId);
            ChatParticipant recipientParticipant = ChatParticipant.create(chatRoomId, recipientId);

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, senderId)).willReturn(true);
            given(messageRepository.save(any(Message.class))).willReturn(saved);
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(chatRoomId))
                    .willReturn(List.of(senderParticipant, recipientParticipant));

            // when
            messageService.createMessage(senderId, chatRoomId, request);

            // then
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            then(applicationEventPublisher).should(times(1)).publishEvent(captor.capture());

            NotificationEvent event = captor.getValue();
            assertThat(event.getRecipientMemberId()).isEqualTo(recipientId);
            assertThat(event.getType()).isEqualTo("CHAT_NEW_MESSAGE");
            assertThat(event.getPayload()).containsEntry("roomId", chatRoomId);
            assertThat(event.getPayload()).containsEntry("senderMemberId", senderId);
        }

        @Test
        @DisplayName("그룹 채팅에서 발신자 외 2명에게 각각 NotificationEvent가 발행된다")
        void publishesNotificationToMultipleParticipants() {
            // given
            Long senderId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", chatRoomId);

            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("안녕하세요");
            request.setMessageType("USER");
            request.setClientMessageId("msg-001");

            Message saved = Message.create(chatRoomId, senderId, "안녕하세요", ChatMessageType.USER, "msg-001");
            ReflectionTestUtils.setField(saved, "id", 100L);

            ChatParticipant p1 = ChatParticipant.create(chatRoomId, senderId);
            ChatParticipant p2 = ChatParticipant.create(chatRoomId, 2L);
            ChatParticipant p3 = ChatParticipant.create(chatRoomId, 3L);

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, senderId)).willReturn(true);
            given(messageRepository.save(any(Message.class))).willReturn(saved);
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(chatRoomId))
                    .willReturn(List.of(p1, p2, p3));

            // when
            messageService.createMessage(senderId, chatRoomId, request);

            // then
            ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
            then(applicationEventPublisher).should(times(2)).publishEvent(captor.capture());

            List<NotificationEvent> events = captor.getAllValues();
            assertThat(events).extracting(NotificationEvent::getRecipientMemberId)
                    .containsExactlyInAnyOrder(2L, 3L);
        }

        @Test
        @DisplayName("참가자가 발신자 혼자면 NotificationEvent가 발행되지 않는다")
        void noNotificationWhenSenderIsOnlyParticipant() {
            // given
            Long senderId = 1L;
            Long chatRoomId = 10L;

            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", chatRoomId);

            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("안녕하세요");
            request.setMessageType("USER");
            request.setClientMessageId("msg-001");

            Message saved = Message.create(chatRoomId, senderId, "안녕하세요", ChatMessageType.USER, "msg-001");
            ReflectionTestUtils.setField(saved, "id", 100L);

            ChatParticipant senderOnly = ChatParticipant.create(chatRoomId, senderId);

            given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(room));
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, senderId)).willReturn(true);
            given(messageRepository.save(any(Message.class))).willReturn(saved);
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(chatRoomId))
                    .willReturn(List.of(senderOnly));

            // when
            messageService.createMessage(senderId, chatRoomId, request);

            // then
            then(applicationEventPublisher).should(never()).publishEvent(any(NotificationEvent.class));
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

            // 리포지토리는 DESC 순서로 반환 (id: 30 → 20 → 10)
            Message msg30 = Message.create(chatRoomId, 2L, "최신메시지", ChatMessageType.USER, "c3");
            ReflectionTestUtils.setField(msg30, "id", 30L);
            Message msg20 = Message.create(chatRoomId, 2L, "중간메시지", ChatMessageType.USER, "c2");
            ReflectionTestUtils.setField(msg20, "id", 20L);
            Message msg10 = Message.create(chatRoomId, 2L, "오래된메시지", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(msg10, "id", 10L);

            given(messageRepository.findByRoomIdWithCursor(chatRoomId, null, 3, "before"))
                    .willReturn(List.of(msg30, msg20, msg10));

            // when
            CursorResponse<ChatMessageResponse> response = messageService.getMessages(memberId, chatRoomId, null, size, null);

            // then
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.isHasMore()).isTrue();
            // nextCursor는 DESC 순서에서 마지막(=가장 오래된) 메시지 ID
            assertThat(response.getNextCursor()).isEqualTo("20");
        }

        @Test
        @DisplayName("DESC로 조회된 메시지를 ASC(시간순)으로 뒤집어 반환한다")
        void returnsMessagesInAscOrder() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);

            // 리포지토리는 DESC 순서로 반환 (id: 30 → 20 → 10)
            Message msg30 = Message.create(chatRoomId, 2L, "최신메시지", ChatMessageType.USER, "c3");
            ReflectionTestUtils.setField(msg30, "id", 30L);
            Message msg20 = Message.create(chatRoomId, 2L, "중간메시지", ChatMessageType.USER, "c2");
            ReflectionTestUtils.setField(msg20, "id", 20L);
            Message msg10 = Message.create(chatRoomId, 2L, "오래된메시지", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(msg10, "id", 10L);

            given(messageRepository.findByRoomIdWithCursor(chatRoomId, null, 51, "before"))
                    .willReturn(List.of(msg30, msg20, msg10));

            // when
            CursorResponse<ChatMessageResponse> response = messageService.getMessages(memberId, chatRoomId, null, null, null);

            // then — 클라이언트에는 ASC(시간순: 10 → 20 → 30) 순서로 반환
            List<ChatMessageResponse> content = response.getContent();
            assertThat(content).hasSize(3);
            assertThat(content.get(0).getId()).isEqualTo(10L);
            assertThat(content.get(1).getId()).isEqualTo(20L);
            assertThat(content.get(2).getId()).isEqualTo(30L);
            assertThat(content.get(0).getContent()).isEqualTo("오래된메시지");
            assertThat(content.get(2).getContent()).isEqualTo("최신메시지");
        }

        @Test
        @DisplayName("hasMore일 때도 ASC 순서로 반환하고 nextCursor는 가장 오래된 메시지 ID이다")
        void hasMore_returnsAscOrderAndCorrectCursor() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;
            int size = 2;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);

            // 리포지토리는 DESC 순서로 3건 반환 (pageSize+1로 hasMore 감지)
            Message msg30 = Message.create(chatRoomId, 2L, "최신", ChatMessageType.USER, "c3");
            ReflectionTestUtils.setField(msg30, "id", 30L);
            Message msg20 = Message.create(chatRoomId, 2L, "중간", ChatMessageType.USER, "c2");
            ReflectionTestUtils.setField(msg20, "id", 20L);
            Message msg10 = Message.create(chatRoomId, 2L, "오래된", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(msg10, "id", 10L);

            given(messageRepository.findByRoomIdWithCursor(chatRoomId, null, 3, "before"))
                    .willReturn(List.of(msg30, msg20, msg10));

            // when
            CursorResponse<ChatMessageResponse> response = messageService.getMessages(memberId, chatRoomId, null, size, null);

            // then — contentRows는 [msg30, msg20] → reverse → [msg20, msg30]
            List<ChatMessageResponse> content = response.getContent();
            assertThat(content).hasSize(2);
            assertThat(content.get(0).getId()).isEqualTo(20L);
            assertThat(content.get(1).getId()).isEqualTo(30L);

            // nextCursor는 reverse 전 DESC 순서의 마지막 = msg20 (id=20)
            assertThat(response.getNextCursor()).isEqualTo("20");
            assertThat(response.isHasMore()).isTrue();
        }

        @Test
        @DisplayName("참여자가 아니면 ROOM_ACCESS_DENIED 예외가 발생한다")
        void nonParticipant_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> messageService.getMessages(memberId, chatRoomId, null, null, null))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.ROOM_ACCESS_DENIED);
        }

        @Test
        @DisplayName("유효하지 않은 커서 값이면 INVALID_CURSOR 예외가 발생한다")
        void invalidCursor_throwsException() {
            // given
            Long memberId = 1L;
            Long chatRoomId = 10L;

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> messageService.getMessages(memberId, chatRoomId, "abc", null, null))
                    .isInstanceOf(ChatException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.INVALID_CURSOR);
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
