package scit.ainiinu.pet.dto.response;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.pet.entity.Pet;

@Getter
@Builder
public class MainPetChangeResponse {
    private Long id;
    private String name;
    private Boolean isMain;

    public static MainPetChangeResponse from(Pet pet) {
        return MainPetChangeResponse.builder()
                .id(pet.getId())
                .name(pet.getName())
                .isMain(pet.getIsMain())
                .build();
    }
}
