package scit.ainiinu.member.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 성공 응답 DTO
 */
@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    
    @JsonProperty("isNewMember")
    private boolean isNewMember;
    
    private Long memberId;
}
