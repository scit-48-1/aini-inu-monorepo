package scit.ainiinu.timeline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "타임라인 공개 설정 응답")
public class TimelineSettingsResponse {

    @Schema(description = "타임라인 공개 여부", example = "true")
    private boolean isTimelinePublic;
}
