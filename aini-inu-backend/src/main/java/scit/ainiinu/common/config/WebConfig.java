package scit.ainiinu.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;

import java.util.List;

/**
 * Spring MVC 웹 설정
 *
 * - Interceptor 등록: JWT 인증 처리
 * - ArgumentResolver 등록: @CurrentMember 파라미터 주입
 * - CORS 설정: 프론트엔드 도메인 허용
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

    /**
     * Interceptor 등록
     *
     * /api/** 경로에 JWT 인증 적용
     * Swagger UI, API Docs, Health Check는 제외
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/v3/api-docs/**",      // Swagger API Docs
                        "/swagger-ui/**",       // Swagger UI
                        "/swagger-ui.html",     // Swagger UI HTML
                        "/actuator/health"      // Health Check
                );
    }

    /**
     * ArgumentResolver 등록
     *
     * @CurrentMember 어노테이션 처리
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }

    /**
     * CORS 설정
     *
     * 프론트엔드에서 API 호출 시 CORS 에러 방지
     * - Local development: localhost:3000 (React), localhost:5173 (Vite)
     * - Production: 실제 프론트엔드 도메인으로 변경 필요
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:3000",     // React 개발 서버
                        "http://localhost:5173"      // Vite 개발 서버
                        // Production: "https://ainiinu.com" 추가
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")  // 프론트엔드에서 Authorization 헤더 읽기 허용
                .allowCredentials(true)           // 쿠키 전송 허용 (필요 시)
                .maxAge(3600);                    // Preflight 캐시: 1시간
    }
}
