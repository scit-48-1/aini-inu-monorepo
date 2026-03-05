package scit.ainiinu.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import scit.ainiinu.member.entity.enums.MemberType;

@Getter
@Setter
@NoArgsConstructor
public class MemberSignupRequest {

    @Schema(
            description = "회원 가입 이메일입니다.",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @Schema(description = "비밀번호입니다.", example = "<MASKED>", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "비밀번호는 대/소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String password;

    @Schema(
            description = "회원 닉네임입니다.",
            example = "몽이아빠",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
    private String nickname;

    @Schema(
            description = "회원 유형 코드입니다.",
            example = "PET_OWNER",
            allowableValues = {"PET_OWNER", "NON_PET_OWNER", "ADMIN"}
    )
    private MemberType memberType;
}
