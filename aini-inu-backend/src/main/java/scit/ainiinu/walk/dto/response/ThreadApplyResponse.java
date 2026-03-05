package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ThreadApplyResponse {
    @Schema(description = "산책 모집글 ID입니다.", example = "101")
    private Long threadId;
    @Schema(description = "채팅방 ID입니다.", example = "101")
    private Long chatRoomId;
    @Schema(description = "신청 상태 코드입니다.", example = "JOINED", allowableValues = {"JOINED", "CANCELED"})
    private String applicationStatus;

    @JsonProperty("isIdempotentReplay")
    @Schema(description = "isIdempotentReplay 값입니다.", example = "true")
    private boolean isIdempotentReplay;
}
