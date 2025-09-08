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
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
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
        private final ClientRegistrationRepository clientRegistrationRepository;
        private final CustomOidcUserService customOidcUserService;
        private final AuthService authService;
        private final ObjectMapper mapper;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                        .csrf(csrf -> csrf.disable())
                        .httpBasic(b -> b.disable())
                        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        // 네이티브 로그인 허용
                                        "/api/auth/login/kakao",
                                        "/api/auth/login/apple",
                                        "/api/auth/login/failure",
                                        "/api/auth/refresh",
                                        // 헬스/문서
                                        "/actuator/health", "/actuator/health/**",
                                        "/swagger-ui/**", "/swagger-ui.html",
                                        "/v3/api-docs/**", "/v3/api-docs",
                                        "/swagger-resources/**", "/webjars/**",
                                        // 구글 웹 OAuth 시작/콜백
                                        "/oauth2/authorization/**", "/login/oauth2/code/**"
                                ).permitAll()
                                .anyRequest().authenticated()
                        )
                        .oauth2Login(oauth2 -> oauth2
                                .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                                .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                                .userInfoEndpoint(u -> u
                                        .userService(oauth2UserService)
                                        .oidcUserService(customOidcUserService) // OIDC(구글)도 커스텀 통일
                                )
                                // 성공/실패 응답을 전부 ApiResponse 포맷으로 통일
                                .successHandler(this::onSuccess)
                                .failureHandler(this::onFailure)
                        )
                        .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /** OAuth2 로그인 성공 응답: ApiResponse 래핑 */
        private void onSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
                CustomOAuth2User oauthUser = (CustomOAuth2User) auth.getPrincipal();
                User user = oauthUser.getUser();

                Map<String, Object> data = authService.createLoginResponse(user, res); // {nickname, accessToken, refreshToken}

                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                ApiResponse<Map<String, Object>> success = ApiResponse.success(data, "로그인 성공");
                mapper.writeValue(res.getWriter(), success);
        }

        /** OAuth2 로그인 실패 응답: ApiResponse 래핑 */
        private void onFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
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
