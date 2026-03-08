package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import scit.ainiinu.walk.entity.WalkingSession;
import scit.ainiinu.walk.entity.WalkingSessionStatus;

import java.time.LocalDateTime;

@Schema(description = "산책 세션 응답")
public record WalkingSessionResponse(
        @Schema(description = "산책 세션 ID입니다.", example = "1")
        Long id,

        @Schema(description = "산책중인 회원 ID입니다.", example = "42")
        Long memberId,

        @Schema(description = "세션 상태입니다.", example = "ACTIVE", allowableValues = {"ACTIVE", "ENDED"})
        WalkingSessionStatus status,

        @Schema(description = "산책 시작 시각입니다.", example = "2026-03-08T14:30:00")
        LocalDateTime startedAt,

        @Schema(description = "마지막 하트비트 수신 시각입니다. 서버는 이 시각 기준 5분 초과 시 세션을 자동 종료합니다.", example = "2026-03-08T14:35:00")
        LocalDateTime lastHeartbeatAt
) {
    public static WalkingSessionResponse from(WalkingSession session) {
        return new WalkingSessionResponse(
                session.getId(),
                session.getMemberId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getLastHeartbeatAt()
        );
    }
}
