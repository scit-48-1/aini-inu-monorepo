package scit.ainiinu.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller 메서드 파라미터에 인증된 회원 ID를 주입하는 어노테이션
 *
 * JwtAuthInterceptor에서 검증된 memberId를 자동으로 주입받습니다.
 *
 * 사용 예시:
 * <pre>
 * // 필수 인증 (기본값)
 * {@literal @}GetMapping("/posts")
 * public ResponseEntity<ApiResponse<List<Post>>> getPosts(
 *     {@literal @}CurrentMember Long memberId
 * ) {
 *     // memberId는 null이 될 수 없음
 * }
 *
 * // 선택적 인증
 * {@literal @}GetMapping("/posts/{id}")
 * public ResponseEntity<ApiResponse<Post>> getPost(
 *     {@literal @}PathVariable Long id,
 *     {@literal @}CurrentMember(required = false) Long memberId
 * ) {
 *     // memberId는 null일 수 있음 (미인증 사용자의 경우)
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentMember {
    /**
     * 인증 필수 여부
     *
     * true (기본값): memberId가 null이면 예외 발생
     * false: memberId가 null이어도 허용 (선택적 인증)
     */
    boolean required() default true;
}
