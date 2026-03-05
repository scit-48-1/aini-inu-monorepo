package scit.ainiinu.walk.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ThreadApplyResponse {
    private Long threadId;
    private Long chatRoomId;
    private String applicationStatus;

    @JsonProperty("isIdempotentReplay")
    private boolean isIdempotentReplay;
}
