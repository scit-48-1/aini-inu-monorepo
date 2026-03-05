package scit.ainiinu.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MessageReadRequest {

    @NotNull
    private Long messageId;

    private OffsetDateTime readAt;
}
