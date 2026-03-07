package scit.ainiinu.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberReviewResponse {
    @Schema(description = "리뷰 ID입니다.", example = "101")
    private Long id;
    @Schema(description = "리뷰어 회원 ID입니다.", example = "5")
    private Long reviewerId;
    @Schema(description = "리뷰어 닉네임입니다.", example = "산책왕")
    private String reviewerNickname;
    @Schema(description = "리뷰어 프로필 이미지 URL입니다.")
    private String reviewerProfileImageUrl;
    @Schema(description = "별점 (1~5)입니다.", example = "4")
    private Integer score;
    @Schema(description = "코멘트 내용입니다.", example = "시간약속을 잘 지켜요; 친절하고 배려심 넘쳐요")
    private String comment;
    @Schema(description = "생성 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
    private LocalDateTime createdAt;
}
