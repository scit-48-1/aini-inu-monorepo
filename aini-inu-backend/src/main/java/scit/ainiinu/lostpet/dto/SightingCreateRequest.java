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
public class SightingCreateRequest {

    @Schema(description = "목격 사진 URL입니다.", example = "https://cdn.example.com/sample.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String photoUrl;

    @NotNull
    @Schema(description = "목격 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime foundAt;

    @Schema(description = "목격 위치 설명입니다.", example = "서울시 영등포구 여의도 한강공원", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String foundLocation;

    @Schema(description = "추가 메모입니다.", example = "빨간 목줄을 착용하고 있었어요.")
    private String memo;
}
