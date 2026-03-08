package scit.ainiinu.timeline.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "타임라인 공개 설정 요청")
public class TimelineSettingsRequest {

    @NotNull
    @Schema(description = "타임라인 공개 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isTimelinePublic;
}
