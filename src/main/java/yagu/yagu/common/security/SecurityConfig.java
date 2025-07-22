package yagu.yagu.common.security;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
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
    private final AuthService authService;
    private final ObjectMapper mapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .httpBasic().disable()
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/authorization/**", "/api/auth/login/failure").permitAll()
                        .requestMatchers("/api/auth/check", "/api/pets/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(u -> u.userService(oauth2UserService))
                        .successHandler(this::onSuccess)
                        .failureHandler(this::onFailure)
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private void onSuccess(HttpServletRequest req,
                           HttpServletResponse res,
                           Authentication auth)
            throws IOException, ServletException {
        var oauthUser = (CustomOAuth2User) auth.getPrincipal();
        var data = authService.createLoginResponse(oauthUser.getUser());

        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        mapper.writeValue(res.getWriter(), data);
    }

    private void onFailure(HttpServletRequest req,
                           HttpServletResponse res,
                           AuthenticationException ex)
            throws IOException, ServletException {
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        mapper.writeValue(res.getWriter(), Map.of("error", ex.getMessage()));
    }
}
