package scit.ainiinu.walk.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalkingUserResponse(
        Long memberId,
        String nickname,
        String profileImageUrl,
        BigDecimal mannerTemperature,
        LocalDateTime walkingStartedAt
) {
}
