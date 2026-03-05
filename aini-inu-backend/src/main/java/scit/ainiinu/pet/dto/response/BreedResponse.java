package scit.ainiinu.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scit.ainiinu.pet.entity.Breed;
import scit.ainiinu.pet.entity.enums.PetSize;

@Getter
@RequiredArgsConstructor
public class BreedResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private final Long id;
    @Schema(description = "이름입니다.", example = "몽이")
    private final String name;
    @Schema(description = "페이지 크기입니다.", example = "예시 문자열")
    private final PetSize size;

    //엔티티 -> DTO 변환
   public static BreedResponse from(Breed breed) {

       return new BreedResponse(
               breed.getId(),
               breed.getName(),
               breed.getSize()
        );
       }

}
