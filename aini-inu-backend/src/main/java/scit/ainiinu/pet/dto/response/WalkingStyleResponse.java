package scit.ainiinu.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.pet.entity.WalkingStyle;

@Getter
@Builder
public class WalkingStyleResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "이름입니다.", example = "몽이")
    private String name;
    @Schema(description = "코드 문자열입니다.", example = "C002")
    private String code;

    public static WalkingStyleResponse from(WalkingStyle walkingStyle) {
        return WalkingStyleResponse.builder()
                .id(walkingStyle.getId())
                .name(walkingStyle.getName())
                .code(walkingStyle.getCode())
                .build();
    }
}
