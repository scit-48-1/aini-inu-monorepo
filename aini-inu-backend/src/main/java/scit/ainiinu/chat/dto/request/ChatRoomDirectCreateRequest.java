package scit.ainiinu.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRoomDirectCreateRequest {

    @NotNull
    private Long partnerId;
}
