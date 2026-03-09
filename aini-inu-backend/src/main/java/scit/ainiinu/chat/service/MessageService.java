package scit.ainiinu.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.chat.dto.request.ChatMessageCreateRequest;
import scit.ainiinu.chat.dto.request.MessageReadRequest;
import scit.ainiinu.chat.dto.response.ChatMessageResponse;
import scit.ainiinu.chat.dto.response.ChatSenderResponse;
import scit.ainiinu.chat.dto.response.MessageReadResponse;
import scit.ainiinu.chat.entity.ChatMessageType;
import scit.ainiinu.chat.entity.ChatParticipant;
import scit.ainiinu.chat.entity.ChatRoom;
import scit.ainiinu.chat.entity.Message;
import scit.ainiinu.chat.exception.ChatErrorCode;
import scit.ainiinu.chat.exception.ChatException;
import scit.ainiinu.chat.realtime.ChatRealtimeEventHandler;
import scit.ainiinu.chat.repository.ChatParticipantRepository;
import scit.ainiinu.chat.repository.ChatRoomRepository;
import scit.ainiinu.chat.repository.MessageRepository;
import scit.ainiinu.common.event.NotificationEvent;
import scit.ainiinu.common.response.CursorResponse;
import scit.ainiinu.notification.entity.NotificationType;
import scit.ainiinu.notification.service.NotificationService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final ChatRealtimeEventHandler chatRealtimeEventHandler;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final NotificationService notificationService;

    public CursorResponse<ChatMessageResponse> getMessages(
            Long memberId,
            Long chatRoomId,
            String cursor,
            Integer size,
            String direction
    ) {
        validateParticipant(memberId, chatRoomId);

        Long parsedCursor = parseCursor(cursor);
        int pageSize = normalizeSize(size);
        String normalizedDirection = normalizeDirection(direction);

        List<Message> rows = messageRepository.findByRoomIdWithCursor(chatRoomId, parsedCursor, pageSize + 1, normalizedDirection);
        boolean hasMore = rows.size() > pageSize;

        List<Message> contentRows;
        if (hasMore) {
            contentRows = new ArrayList<>(rows.subList(0, pageSize));
        } else {
            contentRows = new ArrayList<>(rows);
        }

        // nextCursor는 DESC 순서의 마지막(=가장 오래된) 메시지 ID → reverse 전에 추출
        String nextCursor = null;
        if (hasMore && !contentRows.isEmpty()) {
            nextCursor = String.valueOf(contentRows.get(contentRows.size() - 1).getId());
        }

        // 클라이언트 표시용 시간순(ASC) 정렬
        Collections.reverse(contentRows);

        List<ChatMessageResponse> content = contentRows.stream()
                .map(this::toResponse)
                .toList();

        return new CursorResponse<>(content, nextCursor, hasMore);
    }

    @Transactional
    public ChatMessageResponse createMessage(Long memberId, Long chatRoomId, ChatMessageCreateRequest request) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
        if (room.isClosed()) {
            throw new ChatException(ChatErrorCode.ROOM_CLOSED);
        }
        validateParticipant(memberId, chatRoomId);

        ChatMessageType messageType = parseMessageType(request.getMessageType());
        Message saved = messageRepository.save(Message.create(
                chatRoomId,
                memberId,
                request.getContent(),
                messageType,
                request.getClientMessageId()
        ));

        chatRoomRepository.updateLastMessageAt(chatRoomId, saved.getSentAt());

        ChatMessageResponse response = toResponse(saved);
        chatRealtimeEventHandler.publishMessageCreated(chatRoomId, response);
        chatRealtimeEventHandler.publishMessageDelivered(chatRoomId, saved.getId(), memberId, OffsetDateTime.now());

        publishNotificationToParticipants(memberId, chatRoomId, saved, request.getContent());

        return response;
    }

    @Transactional
    public MessageReadResponse markRead(Long memberId, Long chatRoomId, MessageReadRequest request) {
        ChatParticipant participant = chatParticipantRepository.findByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED));

        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND));
        if (!message.getChatRoomId().equals(chatRoomId)) {
            throw new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND);
        }

        participant.markRead(request.getMessageId());

        OffsetDateTime updatedAt = request.getReadAt() != null ? request.getReadAt() : OffsetDateTime.now();
        chatRealtimeEventHandler.publishMessageRead(chatRoomId, request.getMessageId(), memberId, updatedAt);

        return MessageReadResponse.builder()
                .roomId(chatRoomId)
                .memberId(memberId)
                .lastReadMessageId(participant.getLastReadMessageId())
                .updatedAt(updatedAt)
                .build();
    }

    private ChatMessageResponse toResponse(Message message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .roomId(message.getChatRoomId())
                .sender(ChatSenderResponse.of(message.getSenderId()))
                .content(message.getContent())
                .messageType(message.getMessageType().name())
                .status("CREATED")
                .clientMessageId(message.getClientMessageId())
                .sentAt(message.getSentAt())
                .build();
    }

    private void validateParticipant(Long memberId, Long chatRoomId) {
        boolean isParticipant = chatParticipantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId);
        if (!isParticipant) {
            throw new ChatException(ChatErrorCode.ROOM_ACCESS_DENIED);
        }
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new ChatException(ChatErrorCode.INVALID_CURSOR);
        }
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return "before";
        }
        String normalized = direction.trim().toLowerCase();
        if (!"before".equals(normalized)) {
            throw new ChatException(ChatErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private ChatMessageType parseMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return ChatMessageType.USER;
        }
        try {
            return ChatMessageType.valueOf(messageType);
        } catch (IllegalArgumentException e) {
            throw new ChatException(ChatErrorCode.INVALID_REQUEST);
        }
    }

    private void publishNotificationToParticipants(Long senderId, Long chatRoomId, Message saved, String content) {
        List<ChatParticipant> participants = chatParticipantRepository
                .findAllByChatRoomIdAndLeftAtIsNull(chatRoomId);
        for (ChatParticipant p : participants) {
            if (!p.getMemberId().equals(senderId)) {
                notificationService.createAndPublish(
                        p.getMemberId(),
                        NotificationType.CHAT_NEW_MESSAGE,
                        "새 메시지",
                        truncateContent(content, 50),
                        chatRoomId,
                        "CHAT_ROOM"
                );
            }
        }
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
