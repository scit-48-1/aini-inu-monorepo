package scit.ainiinu.common.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.exception.CommonErrorCode;
import scit.ainiinu.common.security.annotation.Public;
import scit.ainiinu.common.security.jwt.JwtTokenProvider;

/**
 * JWT 인증을 처리하는 Interceptor
 *
 * 모든 /api/** 요청에 대해:
 * 1. @Public 어노테이션 확인 → 있으면 통과
 * 2. Authorization 헤더에서 Bearer 토큰 추출
 * 3. JwtTokenProvider로 토큰 검증
 * 4. 검증 성공 시 request.setAttribute("memberId", memberId) 설정
 * 5. 검증 실패 시 BusinessException throw → GlobalExceptionHandler가 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MEMBER_ID_ATTRIBUTE = "memberId";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 정적 리소스는 인증 체크하지 않음
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // @Public 어노테이션 확인 (메서드 레벨 → 클래스 레벨)
        if (isPublicEndpoint(handlerMethod)) {
            log.debug("Public endpoint: {}", request.getRequestURI());
            return true;
        }

        // 토큰 추출
        String token = extractToken(request);
        if (token == null) {
            log.warn("Missing Authorization header: {}", request.getRequestURI());
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED); // C101
        }

        // 토큰 검증 및 memberId 추출
        Long memberId = jwtTokenProvider.validateAndGetMemberId(token);

        // Request Attribute에 memberId 저장 (ArgumentResolver에서 사용)
        request.setAttribute(MEMBER_ID_ATTRIBUTE, memberId);

        log.debug("Authenticated request: memberId={}, uri={}", memberId, request.getRequestURI());
        return true;
    }

    /**
     * @Public 어노테이션이 있는지 확인
     *
     * 메서드 레벨 우선, 없으면 클래스 레벨 확인
     */
    private boolean isPublicEndpoint(HandlerMethod handlerMethod) {
        // 메서드 레벨 @Public 확인
        if (handlerMethod.getMethodAnnotation(Public.class) != null) {
            return true;
        }

        // 클래스 레벨 @Public 확인
        return handlerMethod.getBeanType().isAnnotationPresent(Public.class);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     *
     * @param request HTTP 요청
     * @return JWT 토큰 (없으면 null)
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
