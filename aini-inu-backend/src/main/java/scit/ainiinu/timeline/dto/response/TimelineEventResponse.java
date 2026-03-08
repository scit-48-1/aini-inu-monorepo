package scit.ainiinu.timeline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.timeline.entity.TimelineEvent;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "타임라인 이벤트 응답 — 회원의 활동 기록 한 건을 나타냅니다")
public class TimelineEventResponse {

    @Schema(description = "타임라인 이벤트 고유 ID", example = "1")
    private Long id;

    @Schema(
            description = "이벤트 타입. WALK_THREAD_CREATED(산책 모집글 생성), "
                    + "WALKING_SESSION_STARTED(산책 시작), WALKING_SESSION_COMPLETED(산책 완료), "
                    + "POST_CREATED(커뮤니티 게시글 작성), LOST_PET_REPORT_CREATED(실종 신고), "
                    + "SIGHTING_CREATED(목격 제보)",
            example = "POST_CREATED"
    )
    private TimelineEventType eventType;

    @Schema(
            description = "이벤트가 참조하는 원본 리소스의 ID. eventType에 따라 게시글 ID, 산책 스레드 ID, "
                    + "산책 세션 ID, 실종 신고 ID, 목격 제보 ID 중 하나입니다.",
            example = "42"
    )
    private Long referenceId;

    @Schema(
            description = "이벤트 제목. eventType에 따라 다른 값이 들어갑니다: "
                    + "WALK_THREAD_CREATED → 모집글 제목, WALKING_SESSION → '산책', "
                    + "POST_CREATED → 게시글 내용 앞부분, LOST_PET_REPORT_CREATED → 반려동물 이름, "
                    + "SIGHTING_CREATED → 발견 장소",
            example = "한강 산책 모집"
    )
    private String title;

    @Schema(
            description = "이벤트 요약 (최대 500자). 게시글의 경우 본문 앞 100자, 산책 모집의 경우 장소명 등이 들어갑니다. null일 수 있습니다.",
            example = "오늘 날씨가 좋아서 한강에서 산책했어요!"
    )
    private String summary;

    @Schema(
            description = "썸네일 이미지 URL. 게시글 첫 번째 이미지, 실종 신고/목격 제보 사진 등이 들어갑니다. 이미지가 없는 이벤트는 null입니다.",
            example = "https://cdn.ainiinu.com/images/post/123.jpg"
    )
    private String thumbnailUrl;

    @Schema(description = "이벤트가 실제 발생한 시각 (ISO 8601)", example = "2026-03-08T14:30:00")
    private LocalDateTime occurredAt;

    public static TimelineEventResponse from(TimelineEvent event) {
        return TimelineEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .referenceId(event.getReferenceId())
                .title(event.getTitle())
                .summary(event.getSummary())
                .thumbnailUrl(event.getThumbnailUrl())
                .occurredAt(event.getOccurredAt())
                .build();
    }
}
