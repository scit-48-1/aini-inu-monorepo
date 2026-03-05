package scit.ainiinu.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatParticipantPetResponse {
    private Long petId;
    private String name;
}
