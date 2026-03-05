package scit.ainiinu.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import scit.ainiinu.pet.entity.Personality;

@Getter
@AllArgsConstructor
public class PersonalityResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private final Long id;
    @Schema(description = "이름입니다.", example = "몽이")
    private final String name;
    @Schema(description = "코드 문자열입니다.", example = "C002")
    private final String code;

    //엔티티 -> DTO 변환
    public static PersonalityResponse from(Personality personality) {
        return new PersonalityResponse(personality.getId(), personality.getName(), personality.getCode());
    }
}
