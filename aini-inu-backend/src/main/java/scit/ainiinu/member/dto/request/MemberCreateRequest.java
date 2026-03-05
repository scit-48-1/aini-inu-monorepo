package scit.ainiinu.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import scit.ainiinu.member.entity.enums.Gender;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MemberCreateRequest {

    @Schema(
            description = "회원 닉네임입니다.",
            example = "몽이아빠",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
    private String nickname;

    @Schema(description = "프로필 이미지 URL입니다.", example = "https://cdn.example.com/sample.jpg")
    private String profileImageUrl;

    @Size(max = 30, message = "연동 닉네임은 30자 이내여야 합니다.")
    @Schema(description = "linkedNickname 값입니다.", example = "몽이아빠")
    private String linkedNickname;

    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    @Schema(description = "전화번호입니다.", example = "01012345678")
    private String phone;

    @Min(value = 1, message = "나이는 1살 이상이어야 합니다.")
    @Max(value = 100, message = "나이는 100살 이하여야 합니다.")
    @Schema(description = "나이입니다.", example = "20")
    private Integer age;

    @Schema(description = "성별 코드입니다.", example = "UNKNOWN", allowableValues = {"MALE", "FEMALE", "UNKNOWN"})
    private Gender gender;

    @Size(max = 4, message = "MBTI는 4자 이내여야 합니다.")
    @Schema(description = "MBTI 문자열입니다.", example = "예시 문자열")
    private String mbti;

    @Size(max = 50, message = "성격 키워드는 50자 이내여야 합니다.")
    @Schema(description = "성격 설명 문자열입니다.", example = "예시 문자열")
    private String personality;

    @Size(max = 200, message = "자기소개는 200자 이내여야 합니다.")
    @Schema(description = "자기소개 문구입니다.", example = "예시 문자열")
    private String selfIntroduction;

    @Schema(description = "personalityTypeIds 값입니다.", example = "[101,102]")
    private List<Long> personalityTypeIds;
}
