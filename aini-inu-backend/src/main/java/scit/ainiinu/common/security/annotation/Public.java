package scit.ainiinu.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 인증이 필요 없는 Public 엔드포인트를 표시하는 어노테이션
 *
 * - METHOD 레벨: 특정 메서드만 공개
 * - TYPE 레벨: 전체 컨트롤러 공개 (예: AuthController)
 *
 * 사용 예시:
 * <pre>
 * {@literal @}Public
 * {@literal @}PostMapping("/auth/login")
 * public ResponseEntity<ApiResponse<TokenResponse>> login(...) {
 *     // 인증 없이 접근 가능
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Public {
}
