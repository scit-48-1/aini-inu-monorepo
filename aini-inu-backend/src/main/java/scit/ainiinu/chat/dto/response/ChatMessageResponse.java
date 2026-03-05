package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ChatMessageResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "roomId 값입니다.", example = "101")
    private Long roomId;
    @Schema(description = "sender 값입니다.", example = "예시 문자열")
    private ChatSenderResponse sender;
    @Schema(description = "본문 내용입니다.", example = "강아지와 즐거운 산책을 했어요.")
    private String content;
    @Schema(description = "메시지 타입 코드입니다.", example = "USER", allowableValues = {"USER", "SYSTEM"})
    private String messageType;
    @Schema(description = "상태 코드입니다.", example = "CREATED", allowableValues = {"CREATED"})
    private String status;
    @Schema(description = "클라이언트 메시지 멱등 키입니다. 클라이언트가 전송한 값을 그대로 반환합니다.", example = "msg-20260305-0001")
    private String clientMessageId;
    @Schema(description = "sentAt 값입니다.", example = "2026-03-05T01:20:00Z")
    private OffsetDateTime sentAt;
}
