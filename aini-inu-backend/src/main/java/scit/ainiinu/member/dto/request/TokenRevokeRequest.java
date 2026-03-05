package scit.ainiinu.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TokenRevokeRequest {

    @Schema(
            description = "로그아웃 시 폐기할 리프레시 토큰입니다.",
            example = "<REFRESH_TOKEN>",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "refreshToken은 필수입니다.")
    private String refreshToken;
}
