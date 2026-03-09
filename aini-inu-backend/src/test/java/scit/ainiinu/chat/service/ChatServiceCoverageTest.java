package scit.ainiinu.chat.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.chat.dto.request.ChatMessageCreateRequest;
import scit.ainiinu.chat.dto.request.ChatReviewCreateRequest;
import scit.ainiinu.chat.dto.request.MessageReadRequest;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;
import scit.ainiinu.chat.dto.response.ChatReviewResponse;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.dto.response.ChatRoomSummaryResponse;
import scit.ainiinu.chat.dto.response.LeaveRoomResponse;
import scit.ainiinu.chat.dto.response.MyChatReviewResponse;
import scit.ainiinu.chat.dto.response.WalkConfirmResponse;
import scit.ainiinu.chat.entity.ChatMessageType;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatReview;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.ChatRoomOrigin;
import scit.ainiinu.chat.entity.ChatRoomStatus;
import scit.ainiinu.chat.entity.ChatRoomType;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.chat.realtime.ChatRealtimeEventHandler;
import scit.ainiinu.chat.repository.ChatParticipantPetRepository;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatReviewRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.response.CursorResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.member.repository.MemberRepository;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.walk.entity.WalkThread;
import scit.ainiinu.walk.entity.WalkChatType;
import scit.ainiinu.walk.entity.WalkThreadStatus;
import scit.ainiinu.walk.repository.WalkThreadRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceCoverageTest {

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
    private ChatRealtimeEventHandler chatRealtimeEventHandler;
    @Mock
    private ChatReviewRepository chatReviewRepository;
    @Mock
    private WalkThreadRepository walkThreadRepository;
    @Mock
    private scit.ainiinu.notification.service.NotificationService notificationService;

    @InjectMocks
    private ChatRoomService chatRoomService;
    @InjectMocks
    private MessageService messageService;
    @InjectMocks
    private WalkConfirmService walkConfirmService;
    @InjectMocks
    private ChatReviewService chatReviewService;

    @Nested
    @DisplayName("ChatRoomService")
    class ChatRoomServiceCoverage {

        @Test
        @DisplayName("채팅방 목록 조회에 성공한다")
        void getRooms_success() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 10L);
            Slice<ChatRoom> slice = new SliceImpl<>(List.of(room), PageRequest.of(0, 20), false);
            given(chatRoomRepository.findAccessibleRoomsByMemberId(1L, null, null, PageRequest.of(0, 20))).willReturn(slice);
            given(messageRepository.findLastMessagesByRoomIds(List.of(10L))).willReturn(java.util.Collections.emptyMap());
            given(messageRepository.countUnreadByRoomIds(1L, List.of(10L))).willReturn(java.util.Collections.emptyMap());

            SliceResponse<ChatRoomSummaryResponse> response = chatRoomService.getRooms(1L, null, null, PageRequest.of(0, 20));

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getChatRoomId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("채팅방 목록 조회 시 배치 쿼리로 마지막 메시지를 가져온다 (N+1 방지)")
        void getRooms_batchFetchesLastMessages() {
            // given: 3개의 채팅방
            ChatRoom room1 = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room1, "id", 1L);
            ChatRoom room2 = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room2, "id", 2L);
            ChatRoom room3 = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room3, "id", 3L);

            Slice<ChatRoom> slice = new SliceImpl<>(List.of(room1, room2, room3), PageRequest.of(0, 20), false);
            given(chatRoomRepository.findAccessibleRoomsByMemberId(1L, null, null, PageRequest.of(0, 20))).willReturn(slice);

            // 방 1, 3에만 메시지 존재
            Message msg1 = Message.create(1L, 10L, "안녕", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(msg1, "id", 100L);
            Message msg3 = Message.create(3L, 11L, "반가워", ChatMessageType.USER, "c3");
            ReflectionTestUtils.setField(msg3, "id", 300L);
            given(messageRepository.findLastMessagesByRoomIds(List.of(1L, 2L, 3L)))
                    .willReturn(java.util.Map.of(1L, msg1, 3L, msg3));
            given(messageRepository.countUnreadByRoomIds(1L, List.of(1L, 2L, 3L))).willReturn(java.util.Collections.emptyMap());

            // when
            SliceResponse<ChatRoomSummaryResponse> response = chatRoomService.getRooms(1L, null, null, PageRequest.of(0, 20));

            // then: 3개 방 모두 반환, 각각 올바른 last message
            assertThat(response.getContent()).hasSize(3);
            assertThat(response.getContent().get(0).getLastMessage().getContent()).isEqualTo("안녕");
            assertThat(response.getContent().get(1).getLastMessage()).isNull(); // 방 2는 메시지 없음
            assertThat(response.getContent().get(2).getLastMessage().getContent()).isEqualTo("반가워");

            // N+1 방지: findTopByChatRoomIdOrderByIdDesc가 호출되지 않아야 함
            org.mockito.Mockito.verify(messageRepository, org.mockito.Mockito.never())
                    .findTopByChatRoomIdOrderByIdDesc(anyLong());
        }

        @Test
        @DisplayName("채팅방 상세 조회에 성공한다")
        void getRoomDetail_success() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 20L);
            ChatParticipant participant = ChatParticipant.create(20L, 1L);
            ReflectionTestUtils.setField(participant, "id", 100L);

            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(20L, 1L)).willReturn(true);
            given(chatRoomRepository.findById(20L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findAllByChatRoomId(20L)).willReturn(List.of(participant));
            given(chatParticipantPetRepository.findAllByChatParticipantIdIn(List.of(100L))).willReturn(List.of());
            given(messageRepository.findTopByChatRoomIdOrderByIdDesc(20L)).willReturn(Optional.empty());

            ChatRoomDetailResponse response = chatRoomService.getRoomDetail(1L, 20L);

            assertThat(response.getChatRoomId()).isEqualTo(20L);
            assertThat(response.getParticipants()).hasSize(1);
        }

        @Test
        @DisplayName("채팅방 나가기에 성공한다")
        void leaveRoom_success() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 30L);
            ChatParticipant participant = ChatParticipant.create(30L, 1L);

            given(chatRoomRepository.findByIdForUpdate(30L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(30L, 1L))
                    .willReturn(Optional.of(participant));
            given(chatParticipantRepository.countByChatRoomIdAndLeftAtIsNull(30L)).willReturn(0L);

            LeaveRoomResponse response = chatRoomService.leaveRoom(1L, 30L);

            assertThat(response.getRoomId()).isEqualTo(30L);
            assertThat(response.isLeft()).isTrue();
        }
    }

    @Nested
    @DisplayName("MessageService")
    class MessageServiceCoverage {

        @Test
        @DisplayName("메시지 조회에 성공한다")
        void getMessages_success() {
            Message message = Message.create(10L, 1L, "안녕하세요", ChatMessageType.USER, "c1");
            ReflectionTestUtils.setField(message, "id", 101L);
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
            given(messageRepository.findByRoomIdWithCursor(10L, null, 51, "before")).willReturn(List.of(message));

            CursorResponse<ChatMessageResponse> response = messageService.getMessages(1L, 10L, null, 50, "before");

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("메시지 전송에 성공한다")
        void createMessage_success() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 10L);
            Message saved = Message.create(10L, 1L, "메시지", ChatMessageType.USER, "cid-1");
            ReflectionTestUtils.setField(saved, "id", 500L);

            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("메시지");
            request.setMessageType("USER");
            request.setClientMessageId("cid-1");

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
            given(messageRepository.save(any(Message.class))).willReturn(saved);

            ChatMessageResponse response = messageService.createMessage(1L, 10L, request);

            assertThat(response.getId()).isEqualTo(500L);
            assertThat(response.getContent()).isEqualTo("메시지");
        }

        @Test
        @DisplayName("메시지 읽음 처리에 성공한다")
        void markRead_success() {
            ChatParticipant participant = ChatParticipant.create(10L, 1L);
            Message message = Message.create(10L, 2L, "읽음", ChatMessageType.USER, "cid-2");
            ReflectionTestUtils.setField(message, "id", 900L);

            MessageReadRequest request = new MessageReadRequest();
            request.setMessageId(900L);
            request.setReadAt(OffsetDateTime.now());

            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(10L, 1L))
                    .willReturn(Optional.of(participant));
            given(messageRepository.findById(900L)).willReturn(Optional.of(message));

            var response = messageService.markRead(1L, 10L, request);

            assertThat(response.getRoomId()).isEqualTo(10L);
            assertThat(response.getLastReadMessageId()).isEqualTo(900L);
        }
    }

    @Nested
    @DisplayName("WalkConfirmService")
    class WalkConfirmServiceCoverage {

        @Test
        @DisplayName("산책 확정 처리에 성공한다")
        void confirmWalk_success() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 1L);
            ChatParticipant me = ChatParticipant.create(1L, 1L);

            given(chatRoomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(1L, 1L)).willReturn(Optional.of(me));
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(1L)).willReturn(List.of(me));
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            WalkConfirmResponse response = walkConfirmService.confirmWalk(1L, 1L);

            assertThat(response.getMyState()).isEqualTo("CONFIRMED");
            assertThat(response.isAllConfirmed()).isTrue();

            // 전원 확정이므로 모든 참여자에게 WALK_CONFIRM 알림이 발행되어야 한다
            then(notificationService).should().createAndPublish(
                    eq(1L),
                    eq(scit.ainiinu.notification.entity.NotificationType.WALK_CONFIRM),
                    eq("산책 완료"),
                    eq("모든 참여자가 산책 완료를 확인했습니다!"),
                    eq(1L),
                    eq("CHAT_ROOM")
            );
        }

        @Test
        @DisplayName("산책 확정 상태 조회에 성공한다")
        void getWalkConfirm_success() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 2L);
            ChatParticipant me = ChatParticipant.create(2L, 1L);

            given(chatRoomRepository.findById(2L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(2L, 1L)).willReturn(Optional.of(me));
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(2L)).willReturn(List.of(me));

            WalkConfirmResponse response = walkConfirmService.getWalkConfirm(1L, 2L);

            assertThat(response.getRoomId()).isEqualTo(2L);
            assertThat(response.getMyState()).isEqualTo("UNCONFIRMED");
        }

        @Test
        @DisplayName("전원 확정 시 WalkThread 상태가 COMPLETED로 전환된다")
        void confirmWalk_completesWalkThread() {
            Long threadId = 100L;
            ChatRoom room = ChatRoom.create(threadId, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            ReflectionTestUtils.setField(room, "id", 1L);
            ChatParticipant me = ChatParticipant.create(1L, 1L);
            WalkThread walkThread = WalkThread.builder()
                    .authorId(1L).title("산책하자").description("같이 산책해요")
                    .walkDate(LocalDate.now()).startTime(LocalDateTime.now())
                    .chatType(WalkChatType.GROUP).maxParticipants(3)
                    .placeName("공원").latitude(BigDecimal.valueOf(37.5))
                    .longitude(BigDecimal.valueOf(127.0))
                    .status(WalkThreadStatus.RECRUITING).build();

            given(chatRoomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(1L, 1L)).willReturn(Optional.of(me));
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(1L)).willReturn(List.of(me));
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room));
            given(walkThreadRepository.findById(threadId)).willReturn(Optional.of(walkThread));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            WalkConfirmResponse response = walkConfirmService.confirmWalk(1L, 1L);

            assertThat(response.isAllConfirmed()).isTrue();
            assertThat(walkThread.getStatus()).isEqualTo(WalkThreadStatus.COMPLETED);

            // 전원 확정 시 알림이 발행되어야 한다
            then(notificationService).should().createAndPublish(
                    eq(1L),
                    eq(scit.ainiinu.notification.entity.NotificationType.WALK_CONFIRM),
                    eq("산책 완료"),
                    eq("모든 참여자가 산책 완료를 확인했습니다!"),
                    eq(1L),
                    eq("CHAT_ROOM")
            );
        }

        @Test
        @DisplayName("threadId가 null인 경우 WalkThread 조회를 하지 않는다")
        void confirmWalk_nullThreadId_skipsWalkThread() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 1L);
            ChatParticipant me = ChatParticipant.create(1L, 1L);

            given(chatRoomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(1L, 1L)).willReturn(Optional.of(me));
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(1L)).willReturn(List.of(me));
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(room));
            given(memberRepository.findById(1L)).willReturn(Optional.empty());

            WalkConfirmResponse response = walkConfirmService.confirmWalk(1L, 1L);

            assertThat(response.isAllConfirmed()).isTrue();
            verify(walkThreadRepository, never()).findById(anyLong());

            // 전원 확정이므로 알림은 발행되어야 한다
            then(notificationService).should().createAndPublish(
                    eq(1L),
                    eq(scit.ainiinu.notification.entity.NotificationType.WALK_CONFIRM),
                    anyString(), anyString(), eq(1L), eq("CHAT_ROOM")
            );
        }

        @Test
        @DisplayName("산책 확정 취소 처리에 성공한다")
        void cancelWalkConfirm_success() {
            ChatRoom room = ChatRoom.create(null, ChatRoomType.GROUP, ChatRoomStatus.ACTIVE, ChatRoomOrigin.DM, null);
            ReflectionTestUtils.setField(room, "id", 3L);
            ChatParticipant me = ChatParticipant.create(3L, 1L);

            given(chatRoomRepository.findByIdForUpdate(3L)).willReturn(Optional.of(room));
            given(chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(3L, 1L)).willReturn(Optional.of(me));
            given(chatParticipantRepository.findAllByChatRoomIdAndLeftAtIsNull(3L)).willReturn(List.of(me));
            given(chatRoomRepository.findById(3L)).willReturn(Optional.of(room));

            walkConfirmService.cancelWalkConfirm(1L, 3L);

            assertThat(me.getWalkConfirmState().name()).isEqualTo("UNCONFIRMED");
        }
    }

    @Nested
    @DisplayName("ChatReviewService")
    class ChatReviewServiceCoverage {

        @Test
        @DisplayName("리뷰 생성에 성공한다")
        void createReview_success() {
            ChatReviewCreateRequest request = new ChatReviewCreateRequest();
            request.setRevieweeId(2L);
            request.setScore(5);
            request.setComment("좋아요");

            ChatRoom walkRoom = ChatRoom.create(null, ChatRoomType.DIRECT, ChatRoomStatus.ACTIVE, ChatRoomOrigin.WALK, null);
            ReflectionTestUtils.setField(walkRoom, "id", 10L);
            walkRoom.updateWalkConfirmed(true);

            ChatReview saved = ChatReview.create(10L, 1L, 2L, 5, "좋아요");
            ReflectionTestUtils.setField(saved, "id", 77L);

            given(chatRoomRepository.findById(10L)).willReturn(Optional.of(walkRoom));
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(10L, 2L)).willReturn(true);
            given(chatReviewRepository.existsByChatRoomIdAndReviewerIdAndRevieweeId(10L, 1L, 2L)).willReturn(false);
            given(chatReviewRepository.save(any(ChatReview.class))).willReturn(saved);

            ChatReviewResponse response = chatReviewService.createReview(1L, 10L, request);

            assertThat(response.getId()).isEqualTo(77L);
            assertThat(response.getScore()).isEqualTo(5);
        }

        @Test
        @DisplayName("내 리뷰 조회에 성공한다")
        void getMyReview_success() {
            ChatReview saved = ChatReview.create(10L, 1L, 2L, 4, "친절");
            ReflectionTestUtils.setField(saved, "id", 88L);
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
            given(chatReviewRepository.findTopByChatRoomIdAndReviewerIdOrderByCreatedAtDesc(10L, 1L))
                    .willReturn(Optional.of(saved));

            MyChatReviewResponse response = chatReviewService.getMyReview(1L, 10L);

            assertThat(response.isExists()).isTrue();
            assertThat(response.getReview().getId()).isEqualTo(88L);
        }

        @Test
        @DisplayName("리뷰 목록 조회에 성공한다")
        void getReviews_success() {
            ChatReview saved = ChatReview.create(10L, 1L, 2L, 3, "무난");
            ReflectionTestUtils.setField(saved, "id", 99L);
            Slice<ChatReview> slice = new SliceImpl<>(List.of(saved), PageRequest.of(0, 20), false);
            given(chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(10L, 1L)).willReturn(true);
            given(chatReviewRepository.findByChatRoomIdOrderByCreatedAtDescIdDesc(10L, PageRequest.of(0, 20)))
                    .willReturn(slice);

            SliceResponse<ChatReviewResponse> response = chatReviewService.getReviews(1L, 10L, PageRequest.of(0, 20));

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getId()).isEqualTo(99L);
        }
    }
}
