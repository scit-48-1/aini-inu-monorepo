package scit.ainiinu.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardPendingReviewResponse {

    @Schema(description = "채팅방 ID", example = "101")
    private Long chatRoomId;

    @Schema(description = "채팅방 표시 이름", example = "산책 메이트")
    private String displayName;

    @Schema(description = "리뷰 대상 회원 ID", example = "12")
    private Long partnerId;

    @Schema(description = "리뷰 대상 닉네임", example = "멍멍이엄마")
    private String partnerNickname;

    @Schema(description = "리뷰 대상 프로필 이미지 URL", example = "https://cdn.example.com/profile.jpg")
    private String profileImageUrl;
}
