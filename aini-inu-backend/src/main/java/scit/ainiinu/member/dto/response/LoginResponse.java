package scit.ainiinu.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 성공 응답 DTO
 */
@Getter
@Builder
public class LoginResponse {
    @Schema(description = "액세스 토큰 문자열입니다.")
    private String accessToken;
    @Schema(description = "리프레시 토큰 문자열입니다.")
    private String refreshToken;
    @Schema(description = "토큰 타입입니다.", example = "Bearer", allowableValues = {"Bearer"})
    private String tokenType;
    @Schema(description = "expiresIn 값입니다.", example = "101")
    private Long expiresIn;
    
    @JsonProperty("isNewMember")
    @Schema(description = "isNewMember 값입니다.", example = "true")
    private boolean isNewMember;
    
    @Schema(description = "회원 ID입니다.", example = "101")
    private Long memberId;
}
