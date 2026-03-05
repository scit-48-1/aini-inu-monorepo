package scit.ainiinu.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

    @Schema(
            description = "재발급에 사용할 리프레시 토큰입니다.",
            example = "<REFRESH_TOKEN>",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;
}
