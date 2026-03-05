package scit.ainiinu.pet.dto.response;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.pet.entity.Pet;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PetResponse {
    private Long id;
    private String name;
    private BreedResponse breed;
    private Integer age;
    private String gender;
    private String size;
    private String mbti;
    private Boolean isNeutered;
    private String photoUrl;
    private Boolean isMain;
    private Boolean isCertified;
    private List<String> walkingStyles; // Codes
    private List<PersonalityResponse> personalities;
    private LocalDateTime createdAt;

    public static PetResponse from(Pet pet, List<String> walkingStyleCodes, List<PersonalityResponse> personalities) {
        return PetResponse.builder()
                .id(pet.getId())
                .name(pet.getName())
                .breed(BreedResponse.from(pet.getBreed()))
                .age(pet.getAge())
                .gender(pet.getGender().name())
                .size(pet.getSize().name())
                .mbti(pet.getMbti())
                .isNeutered(pet.getIsNeutered())
                .photoUrl(pet.getPhotoUrl())
                .isMain(pet.getIsMain())
                .isCertified(pet.getIsCertified())
                .walkingStyles(walkingStyleCodes)
                .personalities(personalities)
                .createdAt(pet.getCreatedAt())
                .build();
    }
}
