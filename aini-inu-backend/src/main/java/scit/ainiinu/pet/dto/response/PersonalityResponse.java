package scit.ainiinu.pet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import scit.ainiinu.pet.entity.Personality;

@Getter
@AllArgsConstructor
public class PersonalityResponse {
    private final Long id;
    private final String name;
    private final String code;

    //엔티티 -> DTO 변환
    public static PersonalityResponse from(Personality personality) {
        return new PersonalityResponse(personality.getId(), personality.getName(), personality.getCode());
    }
}
