package scit.ainiinu.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
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
 * - CORS 설정: CorsConfigurationSource 빈으로 통합 (Security + MVC 동시 적용)
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
     * CORS 설정 (Security 필터 + MVC 레벨 통합)
     *
     * SecurityConfig에서도 이 빈을 주입받아 사용하므로,
     * preflight(OPTIONS) 요청이 Security 필터에서 차단되지 않습니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",     // Next.js 개발 서버
                "http://localhost:5173"      // Vite 개발 서버
                // Production: "https://ainiinu.com" 추가
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // 모든 경로에 적용
        return source;
    }
}
