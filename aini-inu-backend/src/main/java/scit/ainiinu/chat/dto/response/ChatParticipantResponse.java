package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatParticipantResponse {
    private Long memberId;
    private String walkConfirmState;
    private boolean left;
    private List<ChatParticipantPetResponse> pets;
}
