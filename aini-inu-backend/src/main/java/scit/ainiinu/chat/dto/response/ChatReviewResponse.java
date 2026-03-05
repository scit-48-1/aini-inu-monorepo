package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatReviewResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "채팅방 ID입니다.", example = "101")
    private Long chatRoomId;
    @Schema(description = "reviewerId 값입니다.", example = "101")
    private Long reviewerId;
    @Schema(description = "리뷰 대상 회원 ID입니다.", example = "101")
    private Long revieweeId;
    @Schema(description = "점수입니다.", example = "101")
    private Integer score;
    @Schema(description = "코멘트 내용입니다.", example = "상대방과의 산책 매너가 좋았어요.")
    private String comment;
    @Schema(description = "생성 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime createdAt;
}
