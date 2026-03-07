package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.common.response.SliceResponse;

import java.util.Map;

@Getter
@Builder
public class MemberReviewSummaryResponse {
    @Schema(description = "평균 별점입니다.", example = "4.2")
    private double averageScore;
    @Schema(description = "총 리뷰 수입니다.", example = "15")
    private long totalCount;
    @Schema(description = "별점 분포입니다 (키: 별점, 값: 개수).")
    private Map<Integer, Long> scoreDistribution;
    @Schema(description = "리뷰 목록 (페이지네이션)입니다.")
    private SliceResponse<MemberReviewResponse> reviews;
}
