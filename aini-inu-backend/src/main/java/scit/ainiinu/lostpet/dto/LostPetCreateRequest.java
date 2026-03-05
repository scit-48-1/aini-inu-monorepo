package scit.ainiinu.lostpet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostPetCreateRequest {

    @Schema(description = "실종 반려견 이름입니다.", example = "몽이", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String petName;

    @Schema(description = "견종명입니다.", example = "말티즈")
    private String breed;

    @Schema(description = "실종 당시 대표 사진 URL입니다.", example = "https://cdn.example.com/sample.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String photoUrl;

    @Schema(description = "설명 문구입니다.", example = "한강공원에서 30분 산책 예정입니다.")
    private String description;

    @NotNull
    @Schema(description = "마지막 목격 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime lastSeenAt;

    @Schema(description = "마지막 목격 위치 설명입니다.", example = "서울시 영등포구 여의도 한강공원", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String lastSeenLocation;
}
