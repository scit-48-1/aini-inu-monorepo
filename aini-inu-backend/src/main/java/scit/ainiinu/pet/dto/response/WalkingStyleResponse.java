package scit.ainiinu.pet.dto.response;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.pet.entity.WalkingStyle;

@Getter
@Builder
public class WalkingStyleResponse {
    private Long id;
    private String name;
    private String code;

    public static WalkingStyleResponse from(WalkingStyle walkingStyle) {
        return WalkingStyleResponse.builder()
                .id(walkingStyle.getId())
                .name(walkingStyle.getName())
                .code(walkingStyle.getCode())
                .build();
    }
}
