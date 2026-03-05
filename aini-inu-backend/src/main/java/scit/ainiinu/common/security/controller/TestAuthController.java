package scit.ainiinu.common.security.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import scit.ainiinu.common.response.ApiResponse;
import scit.ainiinu.common.security.annotation.Public;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;

import java.util.Map;

/**
 * JWT 테스트용 임시 컨트롤러
 *
 * WARNING: 이 컨트롤러는 개발/테스트 전용입니다.
 * 프로덕션 배포 전에 반드시 삭제하세요!
 *
 * 사용 예시:
 * curl -X POST http://localhost:8080/api/v1/test/auth/token?memberId=1
 */
@RestController
@RequestMapping("/api/v1/test/auth")
@RequiredArgsConstructor
@Public  // 인증 없이 접근 가능
@Tag(name = "Internal", description = "개발/테스트 보조 API")
public class TestAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 테스트용 Access Token 생성
     *
     * @param memberId 회원 ID (기본값: 1)
     * @return Access Token 및 Refresh Token
     */
    @PostMapping("/token")
    @Operation(summary = "테스트 토큰 발급", description = "개발환경에서 memberId 기준 Access/Refresh Token을 발급합니다.")
    @SecurityRequirements()
    public ResponseEntity<ApiResponse<Map<String, String>>> generateTestToken(
            @RequestParam(defaultValue = "1") Long memberId
    ) {
        String accessToken = jwtTokenProvider.generateAccessToken(memberId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);

        Map<String, String> tokens = Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "memberId", memberId.toString(),
                "message", "⚠️ WARNING: This is a test endpoint. Remove before production!"
        );

        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    /**
     * 현재 인증된 사용자 정보 확인 (테스트용)
     *
     * @param memberId @CurrentMember로 주입된 회원 ID
     * @return 인증된 회원 정보
     */
    @GetMapping("/me")
    @Hidden
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentMember(
            @scit.ainiinu.common.security.annotation.CurrentMember Long memberId
    ) {
        Map<String, Object> info = Map.of(
                "memberId", memberId,
                "message", "Authentication successful! You are logged in.",
                "note", "This endpoint requires a valid JWT token in Authorization header"
        );

        return ResponseEntity.ok(ApiResponse.success(info));
    }
}
