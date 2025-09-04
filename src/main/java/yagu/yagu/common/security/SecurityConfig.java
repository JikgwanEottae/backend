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
        private final CustomOidcUserService oidcUserService;
        private final AppleClientSecretService appleClientSecretService;
        private final ClientRegistrationRepository clientRegistrationRepository;
        private final AuthService authService;
        private final ObjectMapper mapper;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                        .csrf(csrf -> csrf.disable())
                        .httpBasic(basic -> basic.disable())
                        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authorizeHttpRequests(auth -> auth
                                // Health
                                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                                // OAuth2 시작/콜백/토큰 재발급
                                .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**",
                                        "/api/auth/login/failure", "/api/auth/refresh").permitAll()
                                // Swagger (개발용)
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                        "/v3/api-docs/**", "/v3/api-docs",
                                        "/swagger-resources/**", "/webjars/**").permitAll()
                                // 임시 허용
                                .requestMatchers("/api/admin/batch/**").permitAll()
                                // 나머지 보호
                                .anyRequest().authenticated()
                        )
                        .oauth2Login(oauth2 -> oauth2
                                .authorizationEndpoint(a -> a
                                        .baseUri("/oauth2/authorization")
                                        .authorizationRequestResolver(
                                                appleAuthorizationRequestResolver(clientRegistrationRepository)
                                        )
                                )
                                .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                                .userInfoEndpoint(u -> u
                                        .userService(oauth2UserService)
                                        .oidcUserService(oidcUserService)
                                )
                                .tokenEndpoint(t -> t.accessTokenResponseClient(appleAwareAccessTokenClient()))
                                .successHandler(this::onSuccess)
                                .failureHandler(this::onFailure)
                        )
                        .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /** OAuth2 로그인 성공 응답 */
        private void onSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
                CustomOAuth2User oauthUser = (CustomOAuth2User) auth.getPrincipal();
                User user = oauthUser.getUser();

                Map<String, Object> data = authService.createLoginResponse(user, res);

                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                ApiResponse<Map<String, Object>> success = ApiResponse.success(data, "로그인 성공");
                mapper.writeValue(res.getWriter(), success);
        }

        /** OAuth2 로그인 실패 응답 */
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

        /** 토큰 교환 시점에 Apple용 client_secret 동적 주입 */
        @Bean
        public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> appleAwareAccessTokenClient() {
                // 기본(애플 외)
                RestClientAuthorizationCodeTokenResponseClient defaultClient =
                        new RestClientAuthorizationCodeTokenResponseClient();
                defaultClient.setParametersConverter(new DefaultOAuth2TokenRequestParametersConverter<>());

                // 애플: 호출 직전에 client_secret 교체
                RestClientAuthorizationCodeTokenResponseClient appleClient =
                        new RestClientAuthorizationCodeTokenResponseClient();
                appleClient.setParametersConverter(new DefaultOAuth2TokenRequestParametersConverter<>());
                // 네 프로젝트 버전에서 단일 인자(Consumer) 형태
                appleClient.setParametersCustomizer(params ->
                        params.set(OAuth2ParameterNames.CLIENT_SECRET, appleClientSecretService.getClientSecret())
                );

                return request -> {
                        String regId = request.getClientRegistration().getRegistrationId();
                        if ("apple".equals(regId)) {
                                return appleClient.getTokenResponse(request);
                        }
                        return defaultClient.getTokenResponse(request);
                };
        }

        /** Apple만 response_mode=form_post 강제 */
        private OAuth2AuthorizationRequestResolver appleAuthorizationRequestResolver(ClientRegistrationRepository repo) {
                DefaultOAuth2AuthorizationRequestResolver resolver =
                        new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

                resolver.setAuthorizationRequestCustomizer(builder ->
                        builder.attributes(attrs -> {
                                Object regId = attrs.get(OAuth2ParameterNames.REGISTRATION_ID);
                                if ("apple".equals(regId)) {
                                        builder.additionalParameters(params -> params.put("response_mode", "form_post"));
                                }
                        })
                );

                return resolver;
        }
}
