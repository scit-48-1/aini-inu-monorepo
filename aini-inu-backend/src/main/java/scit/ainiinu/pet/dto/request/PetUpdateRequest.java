package scit.ainiinu.pet.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PetUpdateRequest {

    @Size(max = 10, message = "이름은 10자를 초과할 수 없습니다.")
    private String name;

    private LocalDate birthDate;

    private Boolean isNeutered;
    private String mbti;
    private String photoUrl;

    // 성향과 산책 스타일은 ID/Code 리스트로 받습니다.
    private List<Long> personalityIds;
    private List<String> walkingStyles;
    private List<String> walkingStyleCodes;

    public Integer resolveAge() {
        if (birthDate == null) {
            return null;
        }
        return Math.max(0, Period.between(birthDate, LocalDate.now()).getYears());
    }

    public List<String> resolveWalkingStyleCodes() {
        if (walkingStyles != null) {
            return walkingStyles;
        }
        return walkingStyleCodes;
    }
}
