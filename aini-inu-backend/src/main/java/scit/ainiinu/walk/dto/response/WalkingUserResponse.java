package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "산책중인 유저 응답")
public record WalkingUserResponse(
        @Schema(description = "회원 ID입니다.", example = "42")
        Long memberId,

        @Schema(description = "회원 닉네임입니다.", example = "멍멍이집사")
        String nickname,

        @Schema(description = "프로필 이미지 URL입니다. 미설정 시 null입니다.", example = "https://example.com/profile.jpg", nullable = true)
        String profileImageUrl,

        @Schema(description = "매너 온도입니다. 1.0~10.0 범위이며 기본값은 5.0입니다.", example = "5.0")
        BigDecimal mannerTemperature,

        @Schema(description = "산책 시작 시각입니다. 경과 시간 계산에 사용합니다.", example = "2026-03-08T14:30:00")
        LocalDateTime walkingStartedAt
) {
}
