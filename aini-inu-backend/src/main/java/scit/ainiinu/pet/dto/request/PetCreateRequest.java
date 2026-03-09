package scit.ainiinu.pet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import scit.ainiinu.pet.entity.enums.PetGender;
import scit.ainiinu.pet.entity.enums.PetSize;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PetCreateRequest {
    @Schema(
            description = "반려견 이름입니다.",
            example = "몽이",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1
    )
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 10, message = "이름은 10자를 초과할 수 없습니다")
    private String name;

    @NotNull(message = "견종 ID는 필수입니다")
    @Schema(description = "견종 ID입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long breedId;

    @NotNull(message = "생년월일은 필수입니다")
    @Schema(description = "반려견 생년월일입니다.", example = "2023-03-05", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate birthDate;

    @NotNull(message = "성별은 필수입니다")
    @Schema(description = "성별 코드입니다.", example = "MALE", allowableValues = {"MALE", "FEMALE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private PetGender gender;

    @NotNull(message = "크기는 필수입니다")
    @Schema(description = "반려견 체형 크기 코드입니다.", example = "SMALL", allowableValues = {"SMALL", "MEDIUM", "LARGE"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private PetSize size;

    @Size(max = 4, message = "MBTI는 4자를 초과할 수 없습니다")
    @Schema(description = "MBTI 문자열입니다.", example = "예시 문자열")
    private String mbti;

    @NotNull(message = "중성화 여부는 필수입니다")
    @Schema(description = "중성화 여부입니다.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isNeutered;

    @Schema(description = "이미지 URL입니다.", example = "https://cdn.example.com/sample.jpg")
    private String photoUrl;
    
    @Schema(description = "대표 반려견 여부입니다.", example = "true")
    private Boolean isMain;

    @Schema(description = "산책 스타일 코드 목록입니다.", example = "[\"LEISURELY\", \"SOCIAL\"]")
    private List<String> walkingStyles; // List of Codes
    @Schema(description = "반려견 성향 ID 목록입니다.", example = "[101,102]")
    private List<Long> personalityIds; // List of IDs

    public Integer resolveAge() {
        return Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
    }
}
