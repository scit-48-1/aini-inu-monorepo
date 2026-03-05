package scit.ainiinu.walk.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ThreadHotspotResponse {
    @Schema(description = "region 값입니다.", example = "예시 문자열")
    private String region;
    @Schema(description = "개수입니다.", example = "20")
    private Long count;
}
