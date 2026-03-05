package scit.ainiinu.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.pet.entity.Pet;

@Getter
@Builder
public class MainPetChangeResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "이름입니다.", example = "몽이")
    private String name;
    @Schema(description = "대표 반려견 여부입니다.", example = "true")
    private Boolean isMain;

    public static MainPetChangeResponse from(Pet pet) {
        return MainPetChangeResponse.builder()
                .id(pet.getId())
                .name(pet.getName())
                .isMain(pet.getIsMain())
                .build();
    }
}
