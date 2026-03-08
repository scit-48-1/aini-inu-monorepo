package scit.ainiinu.timeline.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "타임라인 공개 설정 변경 요청")
public class TimelineSettingsRequest {

    @NotNull
    @Schema(
            description = "타임라인 공개 여부. true로 설정하면 다른 회원이 내 타임라인을 조회할 수 있고, "
                    + "false로 설정하면 본인만 조회 가능합니다.",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Boolean isTimelinePublic;
}
