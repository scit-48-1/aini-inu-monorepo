package scit.ainiinu.pet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.pet.entity.Pet;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PetResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private Long id;
    @Schema(description = "이름입니다.", example = "몽이")
    private String name;
    @Schema(description = "견종 정보입니다.", example = "예시 문자열")
    private BreedResponse breed;
    @Schema(description = "나이입니다.", example = "3")
    private Integer age;
    @Schema(description = "성별 코드입니다.", example = "MALE", allowableValues = {"MALE", "FEMALE"})
    private String gender;
    @Schema(description = "반려견 체형 크기 코드입니다.", example = "SMALL", allowableValues = {"SMALL", "MEDIUM", "LARGE"})
    private String size;
    @Schema(description = "MBTI 문자열입니다.", example = "예시 문자열")
    private String mbti;
    @Schema(description = "중성화 여부입니다.", example = "true")
    private Boolean isNeutered;
    @Schema(description = "이미지 URL입니다.", example = "https://cdn.example.com/sample.jpg")
    private String photoUrl;
    @Schema(description = "대표 반려견 여부입니다.", example = "true")
    private Boolean isMain;
    @Schema(description = "반려견 인증 완료 여부입니다.", example = "true")
    private Boolean isCertified;
    @Schema(description = "산책 스타일 코드 목록입니다.", example = "[\"LEISURELY\", \"SOCIAL\"]")
    private List<String> walkingStyles; // Codes
    @Schema(description = "반려견 성향 태그 목록입니다.", example = "[\"예시 항목\"]")
    private List<PersonalityResponse> personalities;
    @Schema(description = "생성 시각(UTC)입니다.", example = "2026-03-05T01:20:00Z")
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
