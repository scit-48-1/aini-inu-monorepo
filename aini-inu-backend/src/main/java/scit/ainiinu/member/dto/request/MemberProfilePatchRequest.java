package scit.ainiinu.member.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class MemberProfilePatchRequest {

    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
    private String nickname;

    private String profileImageUrl;

    @Size(max = 30, message = "연동 닉네임은 30자 이내여야 합니다.")
    private String linkedNickname;

    @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
    private String phone;

    @Min(value = 1, message = "나이는 1살 이상이어야 합니다.")
    @Max(value = 100, message = "나이는 100살 이하여야 합니다.")
    private Integer age;

    private Gender gender;

    @Size(max = 4, message = "MBTI는 4자 이내여야 합니다.")
    private String mbti;

    @Size(max = 50, message = "성격 키워드는 50자 이내여야 합니다.")
    private String personality;

    @Size(max = 200, message = "자기소개는 200자 이내여야 합니다.")
    private String selfIntroduction;

    private List<Long> personalityTypeIds;
}
