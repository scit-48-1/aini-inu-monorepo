package scit.ainiinu.pet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "반려견 이름입니다. null이면 변경하지 않습니다.", example = "몽이")
    private String name;

    @Schema(description = "반려견 생년월일입니다. null이면 변경하지 않습니다.", example = "2026-03-05")
    private LocalDate birthDate;

    @Schema(description = "중성화 여부입니다. null이면 변경하지 않습니다.", example = "true")
    private Boolean isNeutered;
    @Schema(description = "반려견 MBTI입니다. null이면 변경하지 않습니다.", example = "예시 문자열")
    private String mbti;
    @Schema(description = "반려견 프로필 이미지 URL입니다. null이면 변경하지 않습니다.", example = "https://cdn.example.com/sample.jpg")
    private String photoUrl;

    // 성향과 산책 스타일은 ID/Code 리스트로 받습니다.
    @Schema(description = "성향 ID 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다.", example = "[101,102]")
    private List<Long> personalityIds;
    @Schema(description = "산책 스타일 코드 목록(레거시 필드)입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다.", example = "[\"예시 항목\"]")
    private List<String> walkingStyles;
    @Schema(description = "산책 스타일 코드 목록(권장 필드)입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다.", example = "[\"예시 항목\"]")
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
