package scit.ainiinu.common.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증된 사용자 정보를 담는 DTO
 *
 * 현재는 memberId만 포함하지만, 향후 필요 시 확장 가능합니다.
 * (예: email, roles, permissions 등)
 */
@Getter
@AllArgsConstructor
public class LoginMember {
    /**
     * 인증된 회원 ID
     */
    @Schema(description = "회원 ID입니다.", example = "101")
    private final Long memberId;

    // 향후 확장 예시:
    // private final String email;
    // private final Set<String> roles;
}
