package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class MessageReadResponse {
    @Schema(description = "roomId 값입니다.", example = "101")
    private Long roomId;
    @Schema(description = "회원 ID입니다.", example = "101")
    private Long memberId;
    @Schema(description = "lastReadMessageId 값입니다.", example = "20")
    private Long lastReadMessageId;
    @Schema(description = "수정 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private OffsetDateTime updatedAt;
}
