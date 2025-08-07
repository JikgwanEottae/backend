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
import yagu.yagu.diary.repository.GameDiaryRepository;
import yagu.yagu.diary.repository.UserStatsRepository;
import yagu.yagu.community.repository.PostRepository;
import yagu.yagu.community.repository.CommentRepository;
import yagu.yagu.community.repository.PostLikeRepository;
import yagu.yagu.community.repository.CommentLikeRepository;
import yagu.yagu.common.jwt.JwtTokenProvider;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
        private final UserRepository userRepo;
        private final GameDiaryRepository gameDiaryRepository;
        private final UserStatsRepository userStatsRepository;
        private final PostRepository postRepository;
        private final CommentRepository commentRepository;
        private final PostLikeRepository postLikeRepository;
        private final CommentLikeRepository commentLikeRepository;

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
         * 회원탈퇴: 연관된 모든 데이터를 수동으로 삭제 후 유저 계정 삭제
         */
        @Transactional
        public void withdraw(User user) {
                Long userId = user.getId();

                // 1. RefreshToken 삭제
                refreshTokenService.deleteByUser(user);

                // 2. 커뮤니티 관련 데이터 삭제 (자식부터 삭제)
                // 2-1. CommentLike 삭제 (Comment에 달린 좋아요)
                commentLikeRepository.deleteByOwner(user);

                // 2-2. PostLike 삭제 (Post에 달린 좋아요)
                postLikeRepository.deleteByOwner(user);

                // 2-3. Comment 삭제 (댓글)
                commentRepository.deleteByOwner(user);

                // 2-4. Post 삭제 (게시글)
                postRepository.deleteByOwner(user);

                // 3. 다이어리 관련 데이터 삭제
                // 3-1. GameDiary 삭제
                gameDiaryRepository.deleteByUser(user);

                // 3-2. UserStats 삭제
                userStatsRepository.deleteById(userId);

                // 4. 마지막으로 User 삭제
                userRepo.delete(user);
        }
}
