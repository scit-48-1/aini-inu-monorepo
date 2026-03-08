package scit.ainiinu.timeline.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "타임라인 공개 설정 응답 — 변경 후 현재 설정 값을 반환합니다")
public class TimelineSettingsResponse {

    @Schema(
            description = "타임라인 공개 여부. true면 타인에게 공개, false면 본인만 조회 가능",
            example = "true"
    )
    private boolean isTimelinePublic;
}
