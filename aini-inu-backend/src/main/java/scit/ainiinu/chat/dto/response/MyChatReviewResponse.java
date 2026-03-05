package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyChatReviewResponse {
    @Schema(description = "exists 값입니다.", example = "true")
    private boolean exists;
    @Schema(description = "review 값입니다.", example = "예시 문자열")
    private ChatReviewResponse review;
}
