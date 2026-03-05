package scit.ainiinu.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 *
 * 이 프로젝트는 Spring Security를 사용하지 않고 커스텀 Interceptor로 인증을 처리합니다.
 * Spring Security는 의존성에 포함되어 있지만, 모든 기본 동작을 비활성화합니다.
 *
 * Spring Security를 포함한 이유:
 * - 향후 BCryptPasswordEncoder 등 유틸리티 사용 가능
 * - CSRF, CORS 등 보안 유틸리티 활용 가능
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 모든 기본 보안 기능 비활성화
                .csrf(AbstractHttpConfigurer::disable)          // CSRF 비활성화
                .formLogin(AbstractHttpConfigurer::disable)     // Form 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)     // HTTP Basic 인증 비활성화
                .logout(AbstractHttpConfigurer::disable)        // 로그아웃 비활성화

                // 모든 요청 허용 (인증은 JwtAuthInterceptor에서 처리)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
