package scit.ainiinu.timeline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.timeline.entity.TimelineEvent;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "타임라인 이벤트 응답")
public class TimelineEventResponse {

    @Schema(description = "이벤트 ID", example = "1")
    private Long id;

    @Schema(description = "이벤트 타입", example = "POST_CREATED")
    private TimelineEventType eventType;

    @Schema(description = "참조 대상 ID", example = "42")
    private Long referenceId;

    @Schema(description = "이벤트 제목", example = "한강 산책")
    private String title;

    @Schema(description = "이벤트 요약", example = "오늘 날씨가 좋아서 한강에서 산책했어요!")
    private String summary;

    @Schema(description = "썸네일 이미지 URL")
    private String thumbnailUrl;

    @Schema(description = "이벤트 발생 시각")
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
