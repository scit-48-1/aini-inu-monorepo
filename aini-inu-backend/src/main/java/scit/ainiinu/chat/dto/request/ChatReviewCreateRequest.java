package scit.ainiinu.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatReviewCreateRequest {

    @NotNull
    @Schema(description = "리뷰 대상 회원 ID입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long revieweeId;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "점수입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer score;

    @Schema(description = "코멘트 내용입니다.", example = "상대방과의 산책 매너가 좋았어요.")
    private String comment;
}
