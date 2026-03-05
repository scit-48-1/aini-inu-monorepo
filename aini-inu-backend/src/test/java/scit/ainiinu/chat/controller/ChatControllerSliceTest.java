package scit.ainiinu.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.chat.dto.request.ChatReviewCreateRequest;
import scit.ainiinu.chat.dto.request.ChatMessageCreateRequest;
import scit.ainiinu.chat.dto.request.ChatRoomDirectCreateRequest;
import scit.ainiinu.chat.dto.request.MessageReadRequest;
import scit.ainiinu.chat.dto.request.WalkConfirmRequest;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;
import scit.ainiinu.chat.dto.response.ChatReviewResponse;
import scit.ainiinu.chat.dto.response.ChatRoomDetailResponse;
import scit.ainiinu.chat.dto.response.ChatRoomSummaryResponse;
import scit.ainiinu.chat.dto.response.ChatSenderResponse;
import scit.ainiinu.chat.dto.response.MessageReadResponse;
import scit.ainiinu.chat.dto.response.MyChatReviewResponse;
import scit.ainiinu.chat.dto.response.WalkConfirmResponse;
import scit.ainiinu.chat.service.ChatReviewService;
import scit.ainiinu.chat.service.ChatRoomService;
import scit.ainiinu.chat.service.MessageService;
import scit.ainiinu.chat.service.WalkConfirmService;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import scit.ainiinu.common.response.CursorResponse;
import scit.ainiinu.common.response.SliceResponse;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(GlobalExceptionHandler.class)
class ChatControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private WalkConfirmService walkConfirmService;

    @MockitoBean
    private ChatReviewService chatReviewService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(currentMemberArgumentResolver.supportsParameter(
                argThat(parameter -> parameter.hasParameterAnnotation(CurrentMember.class))
        )).willReturn(true);
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Nested
    @DisplayName("CHAT-ROOM-DIRECT-CREATE 계약")
    class DirectCreateContract {

        @Test
        @WithMockUser
        @DisplayName("성공: direct 채팅방 생성/재사용 응답을 반환한다")
        void createDirect_success() throws Exception {

            // given
            ChatRoomDirectCreateRequest request = new ChatRoomDirectCreateRequest();
            request.setPartnerId(2L);

            ChatRoomDetailResponse response = ChatRoomDetailResponse.builder()
                    .chatRoomId(100L)
                    .chatType("DIRECT")
                    .status("ACTIVE")
                    .walkConfirmed(false)
                    .participants(List.of())
                    .build();

            given(chatRoomService.createDirectRoom(anyLong(), any(ChatRoomDirectCreateRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/direct")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.chatRoomId").value(100L));
        }
    }

    @Nested
    @DisplayName("CHAT-ROOMS/DETAIL/LEAVE 계약")
    class RoomContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 채팅방 목록을 SliceResponse로 반환한다")
        void getChatRooms_success() throws Exception {
            ChatRoomSummaryResponse room = ChatRoomSummaryResponse.builder()
                    .chatRoomId(100L)
                    .chatType("DIRECT")
                    .status("ACTIVE")
                    .updatedAt(LocalDateTime.now())
                    .build();
            Slice<ChatRoomSummaryResponse> slice = new SliceImpl<>(List.of(room), PageRequest.of(0, 20), false);
            given(chatRoomService.getRooms(anyLong(), any(), any())).willReturn(SliceResponse.of(slice));

            mockMvc.perform(get("/api/v1/chat-rooms")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].chatRoomId").value(100L));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 채팅방 상세를 반환한다")
        void getChatRoom_success() throws Exception {
            ChatRoomDetailResponse detail = ChatRoomDetailResponse.builder()
                    .chatRoomId(100L)
                    .chatType("DIRECT")
                    .status("ACTIVE")
                    .walkConfirmed(false)
                    .participants(List.of())
                    .build();
            given(chatRoomService.getRoomDetail(anyLong(), anyLong())).willReturn(detail);

            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.chatRoomId").value(100L));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 채팅방 나가기 응답을 반환한다")
        void leaveRoom_success() throws Exception {
            given(chatRoomService.leaveRoom(anyLong(), anyLong())).willReturn(
                    scit.ainiinu.chat.dto.response.LeaveRoomResponse.builder()
                            .roomId(100L)
                            .left(true)
                            .roomStatus("ACTIVE")
                            .build()
            );

            mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/leave", 100L).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.roomId").value(100L))
                    .andExpect(jsonPath("$.data.left").value(true));
        }
    }

    @Nested
    @DisplayName("CHAT-MSG-LIST 계약")
    class MessageListContract {

        @Test
        @WithMockUser
        @DisplayName("성공: CursorResponse 형식으로 최신순 메시지를 반환한다")
        void listMessages_success() throws Exception {
            // given
            ChatMessageResponse message = ChatMessageResponse.builder()
                    .id(301L)
                    .roomId(100L)
                    .sender(ChatSenderResponse.of(1L))
                    .content("안녕하세요")
                    .messageType("USER")
                    .status("CREATED")
                    .sentAt(OffsetDateTime.now())
                    .build();
            CursorResponse<ChatMessageResponse> cursorResponse = new CursorResponse<>(List.of(message), "300", true);

            given(messageService.getMessages(anyLong(), anyLong(), any(), any(), any())).willReturn(cursorResponse);

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/messages", 100L)
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(301L))
                    .andExpect(jsonPath("$.data.nextCursor").value("300"))
                    .andExpect(jsonPath("$.data.hasMore").value(true));
        }
    }

    @Nested
    @DisplayName("CHAT-MSG-SEND 계약")
    class MessageSendContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 메시지 전송 응답을 반환한다")
        void createMessage_success() throws Exception {
            ChatMessageCreateRequest request = new ChatMessageCreateRequest();
            request.setContent("메시지 전송");
            request.setMessageType("USER");
            request.setClientMessageId("c1");

            ChatMessageResponse response = ChatMessageResponse.builder()
                    .id(400L)
                    .roomId(100L)
                    .sender(ChatSenderResponse.of(1L))
                    .content("메시지 전송")
                    .messageType("USER")
                    .status("CREATED")
                    .sentAt(OffsetDateTime.now())
                    .build();
            given(messageService.createMessage(anyLong(), anyLong(), any(ChatMessageCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages", 100L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(400L))
                    .andExpect(jsonPath("$.data.content").value("메시지 전송"));
        }
    }

    @Nested
    @DisplayName("CHAT-WALK-CONFIRM 계약")
    class WalkConfirmContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 산책확인 상태 응답을 반환한다")
        void walkConfirm_success() throws Exception {
            // given
            WalkConfirmRequest request = new WalkConfirmRequest();
            request.setAction("CONFIRM");

            WalkConfirmResponse response = WalkConfirmResponse.builder()
                    .roomId(100L)
                    .memberId(1L)
                    .myState("CONFIRMED")
                    .allConfirmed(false)
                    .confirmedMemberIds(List.of(1L, 2L))
                    .build();

            given(walkConfirmService.updateWalkConfirm(anyLong(), anyLong(), any(WalkConfirmRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 100L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.myState").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.confirmedMemberIds[1]").value(2L));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 산책확인 상태 조회 응답을 반환한다")
        void getWalkConfirm_success() throws Exception {
            WalkConfirmResponse response = WalkConfirmResponse.builder()
                    .roomId(100L)
                    .memberId(1L)
                    .myState("CONFIRMED")
                    .allConfirmed(false)
                    .confirmedMemberIds(List.of(1L))
                    .build();

            given(walkConfirmService.getWalkConfirm(anyLong(), anyLong()))
                    .willReturn(response);

            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.myState").value("CONFIRMED"));
        }

        @Test
        @WithMockUser
        @DisplayName("성공: 산책확인 취소는 null data를 반환한다")
        void deleteWalkConfirm_success() throws Exception {
            willDoNothing().given(walkConfirmService).cancelWalkConfirm(anyLong(), anyLong());

            mockMvc.perform(delete("/api/v1/chat-rooms/{chatRoomId}/walk-confirm", 100L).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("CHAT-REVIEW 계약")
    class ReviewContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 리뷰 생성 후 조회 응답을 반환한다")
        void createAndGetReview_success() throws Exception {
            // given
            ChatReviewCreateRequest createRequest = new ChatReviewCreateRequest();
            createRequest.setRevieweeId(2L);
            createRequest.setScore(5);
            createRequest.setComment("친절했어요");

            ChatReviewResponse created = ChatReviewResponse.builder()
                    .id(77L)
                    .chatRoomId(100L)
                    .reviewerId(1L)
                    .revieweeId(2L)
                    .score(5)
                    .comment("친절했어요")
                    .createdAt(LocalDateTime.now())
                    .build();

            Slice<ChatReviewResponse> slice = new SliceImpl<>(List.of(created), PageRequest.of(0, 20), false);

            given(chatReviewService.createReview(anyLong(), anyLong(), any(ChatReviewCreateRequest.class))).willReturn(created);
            given(chatReviewService.getMyReview(anyLong(), anyLong()))
                    .willReturn(MyChatReviewResponse.builder().exists(true).review(created).build());
            given(chatReviewService.getReviews(anyLong(), anyLong(), any())).willReturn(SliceResponse.of(slice));

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/reviews", 100L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(77L));

            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/reviews/me", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.exists").value(true));

            mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/reviews", 100L)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].score").value(5));
        }
    }

    @Nested
    @DisplayName("CHAT-MSG-READ 계약")
    class MessageReadContract {

        @Test
        @WithMockUser
        @DisplayName("성공: 읽음 워터마크를 갱신한다")
        void markRead_success() throws Exception {
            // given
            MessageReadRequest request = new MessageReadRequest();
            request.setMessageId(301L);
            request.setReadAt(OffsetDateTime.now());

            MessageReadResponse response = MessageReadResponse.builder()
                    .roomId(100L)
                    .memberId(1L)
                    .lastReadMessageId(301L)
                    .updatedAt(OffsetDateTime.now())
                    .build();

            given(messageService.markRead(anyLong(), anyLong(), any(MessageReadRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/messages/read", 100L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lastReadMessageId").value(301L));
        }
    }
}
