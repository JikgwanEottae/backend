package yagu.yagu.user.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.common.jwt.JwtConfig;
import yagu.yagu.common.jwt.RefreshToken;
import yagu.yagu.common.jwt.RefreshTokenService;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;
import yagu.yagu.common.jwt.JwtTokenProvider;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtConfig jwtConfig;

    /**
     * OAuth2 로그인 성공 시 호출
     */
    public Map<String, Object> createLoginResponse(User user,
                                                   HttpServletResponse response) {
        // 1) Access Token
        String accessToken = jwtProvider.createToken(user.getEmail());

        // 2) Refresh Token
        RefreshToken refresh = refreshTokenService.createRefreshToken(user);

        // 3) HttpOnly 쿠키로 Refresh Token 전달
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtConfig.getRefreshExpiration() / 1000)
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 4) 응답 바디용 Map 구성
        return Map.of(
                "accessToken", accessToken,
                "user", Map.of(
                        "email", user.getEmail(),
                        "nickname", user.getNickname(),
                        "provider", user.getProvider(),
                        "profileCompleted", user.isProfileCompleted()
                )
        );
    }

    /** 기존 회원 조회 or 신규 생성 (profileCompleted=false) */
    public User findOrCreateUser(String email, String nickname,
                                 User.AuthProvider provider, String providerId) {
        return userRepo.findByEmail(email)
                .orElseGet(() -> userRepo.save(
                        User.builder()
                                .email(email)
                                .nickname(nickname)
                                .provider(provider)
                                .providerId(providerId)
                                .profileCompleted(false)
                                .build()
                ));
    }
}
