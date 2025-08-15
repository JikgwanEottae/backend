package yagu.yagu.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import yagu.yagu.common.jwt.JwtAuthenticationFilter;
import yagu.yagu.common.jwt.JwtTokenProvider;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.service.AuthService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtTokenProvider jwtProvider;
        private final CustomOAuth2UserService oauth2UserService;
        private final CustomOidcUserService oidcUserService;
        private final AuthService authService;
        private final ObjectMapper mapper;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf().disable()
                                .httpBasic().disable()
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // OAuth2 로그인 시작 및 실패, 토큰 재발급은 인증 없이 접근
                                                .requestMatchers(
                                                                "/oauth2/authorization/**",
                                                                "/login/oauth2/code/**",
                                                                "/api/auth/login/failure",
                                                                "/api/auth/refresh")
                                                .permitAll()
                                                // Swagger 문서 (개발용)
                                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**", "/v3/api-docs",
                                                                "/swagger-resources/**", "/webjars/**")
                                                .permitAll()
                                                // 배치 테스트용 엔드포인트 임시 허용
                                                .requestMatchers("/api/admin/batch/**").permitAll()
                                                // 나머지 모든 API는 JWT 토큰 필요
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                                                .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                                                .userInfoEndpoint(u -> u
                                                                .userService(oauth2UserService)
                                                                .oidcUserService(oidcUserService))
                                                .successHandler(this::onSuccess)
                                                .failureHandler(this::onFailure))
                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtProvider),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * OAuth2 로그인 성공 시 호출됩니다.
         * AuthService#createLoginResponse(User, HttpServletResponse) 에서
         * 액세스 토큰 → JSON 바디, 리프레시 토큰 → HttpOnly 쿠키로 세팅합니다.
         */
        private void onSuccess(HttpServletRequest req,
                        HttpServletResponse res,
                        Authentication auth) throws IOException {
                CustomOAuth2User oauthUser = (CustomOAuth2User) auth.getPrincipal();
                User user = oauthUser.getUser();

                Map<String, Object> data = authService.createLoginResponse(user, res);

                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

                ApiResponse<Map<String, Object>> success = ApiResponse.success(data, "로그인 성공");

                mapper.writeValue(res.getWriter(), success);
        }

        /**
         * OAuth2 로그인 실패 시 호출됩니다.
         */
        private void onFailure(HttpServletRequest req,
                        HttpServletResponse res,
                        AuthenticationException ex) throws IOException {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

                ApiResponse<Object> error = ApiResponse.builder()
                                .result(false)
                                .httpCode(HttpStatus.UNAUTHORIZED.value())
                                .data(null)
                                .message("소셜 로그인 실패: " + ex.getMessage())
                                .build();

                mapper.writeValue(res.getWriter(), error);
        }
}
