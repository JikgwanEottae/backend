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

                // 3) (웹) HttpOnly 쿠키에 저장
                ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh.getToken())
                                .httpOnly(true)
                                .secure(true)
                                .path("/")
                                .maxAge(jwtConfig.getRefreshExpiration() / 1000)
                                .build();
                response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                // 4) JSON 바디에도 둘 다 담아 반환
                return Map.of(
                                "accessToken", accessToken,
                                "refreshToken", refresh.getToken(), // ← 추가
                                "user", Map.of(
                                                "email", user.getEmail(),
                                                "nickname", user.getNickname(),
                                                "provider", user.getProvider(),
                                                "profileCompleted", user.isProfileCompleted()));
        }

        /** 기존 회원 조회 or 신규 생성 (profileCompleted=false) */
        public User findOrCreateUser(String email, String nickname,
                        User.AuthProvider provider, String providerId) {
                return userRepo.findByEmail(email)
                                .orElseGet(() -> userRepo.save(
                                                // Builder 대신 정적 팩토리로 생성
                                                User.of(email, nickname, provider, providerId)));
        }

        /**
         * 로그아웃: RefreshToken 삭제
         */
        @Transactional
        public void logout(User user) {
                refreshTokenService.deleteByUser(user);
        }

        /**
         * 회원탈퇴: CASCADE 설정을 활용하여 연관된 모든 데이터와 함께 유저 삭제
         */
        @Transactional
        public void withdraw(User user) {

                userRepo.delete(user);
        }
}
