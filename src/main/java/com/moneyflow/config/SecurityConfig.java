package com.moneyflow.config;

import com.moneyflow.security.CustomAuthenticationEntryPoint;
import com.moneyflow.security.CustomUserDetailsService;
import com.moneyflow.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final Environment environment;

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> {
                    // 🔐 개발용 엔드포인트: dev 프로파일에서만 허용, 그 외에는 완전 차단
                    if (!isDevProfile()) {
                        auth.requestMatchers(
                                "/api/auth/dev/**",
                                "/api/auth/social-login/mock",
                                "/api/test/**").denyAll();
                    }

                    auth.requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/talmo/**").permitAll() // Talmo 타이핑 연습 (인증 불필요)
                            .requestMatchers("/api/test/**").permitAll() // 테스트 엔드포인트(dev 전용)
                            .requestMatchers(
                                    "/swagger-ui.html",
                                    "/swagger-ui/**",
                                    "/v3/api-docs/**",
                                    "/api-docs/**",
                                    "/swagger-resources/**",
                                    "/webjars/**")
                            .permitAll()
                            .requestMatchers("/actuator/health").permitAll()
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 환경변수로 CORS Origin 제어
        // dev: "*" (모든 Origin 허용)
        // prod: "https://app.moneyflow.com,https://www.moneyflow.com" (특정 도메인만 허용)
        if ("*".equals(allowedOrigins)) {
            // 개발 환경: 모든 Origin 허용 (allowCredentials와 함께 사용 불가)
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            // 프로덕션 환경: 특정 도메인만 허용
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
            configuration.setAllowCredentials(true);
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
