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
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 10, message = "이름은 10자를 초과할 수 없습니다")
    private String name;

    @NotNull(message = "견종 ID는 필수입니다")
    private Long breedId;

    @NotNull(message = "생년월일은 필수입니다")
    private LocalDate birthDate;

    @NotNull(message = "성별은 필수입니다")
    private PetGender gender;

    @NotNull(message = "크기는 필수입니다")
    private PetSize size;

    @Size(max = 4, message = "MBTI는 4자를 초과할 수 없습니다")
    private String mbti;

    @NotNull(message = "중성화 여부는 필수입니다")
    private Boolean isNeutered;

    private String photoUrl;
    
    private Boolean isMain;

    @Size(max = 15, message = "동물등록번호는 15자 이하여야 합니다")
    private String certificationNumber;

    private List<String> walkingStyles; // List of Codes
    private List<Long> personalityIds; // List of IDs

    public Integer resolveAge() {
        return Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
    }
}
