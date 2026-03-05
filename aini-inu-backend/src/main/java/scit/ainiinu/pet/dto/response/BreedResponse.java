package scit.ainiinu.pet.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import scit.ainiinu.pet.entity.Breed;
import scit.ainiinu.pet.entity.enums.PetSize;

@Getter
@RequiredArgsConstructor
public class BreedResponse {
    private final Long id;
    private final String name;
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
