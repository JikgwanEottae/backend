package yagu.yagu.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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

        // API 체인: /api/** → 미인증 시 401 JSON
        @Bean
        @Order(1)
        public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
                http
                        .securityMatcher("/api/**")
                        .csrf(csrf -> csrf.disable())
                        .httpBasic(b -> b.disable())
                        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        "/api/auth/login/kakao",
                                        "/api/auth/login/apple",
                                        "/api/auth/login/failure",
                                        "/api/auth/refresh"
                                ).permitAll()
                                .anyRequest().authenticated()
                        )
                        .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                                mapper.writeValue(res.getWriter(),
                                        ApiResponse.builder()
                                                .result(false)
                                                .httpCode(401)
                                                .data(null)
                                                .message("인증이 필요합니다.")
                                                .build()
                                );
                        }))
                        .oauth2Login(o -> o.disable())
                        .formLogin(f -> f.disable())
                        .logout(l -> l.disable());

                // JWT 필터 연결
                http.addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        //  웹(리다이렉트 로그인) 체인: 나머지 경로
        @Bean
        @Order(2)
        public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
                http
                        .csrf(csrf -> csrf.disable())
                        .httpBasic(b -> b.disable())
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        "/actuator/health", "/actuator/health/**",
                                        "/swagger-ui/**", "/swagger-ui.html",
                                        "/v3/api-docs/**", "/v3/api-docs",
                                        "/swagger-resources/**", "/webjars/**",
                                        "/oauth2/authorization/**", "/login/oauth2/code/**"
                                ).permitAll()
                                .anyRequest().authenticated()
                        )
                        .oauth2Login(oauth2 -> oauth2
                                .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                                .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                                .userInfoEndpoint(u -> u
                                        .userService(oauth2UserService)
                                        .oidcUserService(customOidcUserService)
                                )
                                .successHandler(this::onSuccess)
                                .failureHandler(this::onFailure)
                        );

                return http.build();
        }

        private void onSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
                CustomOAuth2User oauthUser = (CustomOAuth2User) auth.getPrincipal();
                User user = oauthUser.getUser();
                Map<String, Object> data = authService.createLoginResponse(user, res);
                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                mapper.writeValue(res.getWriter(), ApiResponse.success(data, "로그인 성공"));
        }

        private void onFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
                res.setStatus(HttpStatus.UNAUTHORIZED.value());
                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                mapper.writeValue(res.getWriter(),
                        ApiResponse.builder()
                                .result(false)
                                .httpCode(401)
                                .data(null)
                                .message("소셜 로그인 실패: " + ex.getMessage())
                                .build()
                );
        }
}
