package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomDetailResponse {
    private Long chatRoomId;
    private String chatType;
    private String status;
    private boolean walkConfirmed;
    private List<ChatParticipantResponse> participants;
    private ChatMessageResponse lastMessage;
}
