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
import java.time.Instant;

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

        /**
         * 기존 회원 조회 우선순위
         * 1) 삭제되지 않은 사용자(by email)
         * 2) 30일 내 삭제된 사용자(by deletedOriginalEmail) → 복구
         * 3) 없으면 신규 생성
         */
        public User findOrCreateUser(String email, String nickname,
                        User.AuthProvider provider, String providerId) {
                var activeOpt = userRepo.findByEmailAndDeletedAtIsNull(email);
                if (activeOpt.isPresent())
                        return activeOpt.get();

                var deletedOpt = userRepo.findByDeletedOriginalEmailAndDeletedAtIsNotNull(email);
                if (deletedOpt.isPresent()) {
                        User deletedUser = deletedOpt.get();
                        if (deletedUser.getPurgeAt() == null || deletedUser.getPurgeAt().isAfter(Instant.now())) {
                                deletedUser.restoreFromDeletion(email, nickname);
                                return userRepo.save(deletedUser);
                        }
                }
                return userRepo.save(User.of(email, nickname, provider, providerId));
        }

        /**
         * 로그아웃: RefreshToken 삭제
         */
        @Transactional
        public void logout(User user) {
                refreshTokenService.deleteByUser(user);
        }

        /**
         * 회원탈퇴: 소프트삭제 + 익명화 + 30일 후 영구삭제 예약
         */
        @Transactional
        public void withdraw(User user) {
                // 토큰 무효화
                refreshTokenService.deleteByUser(user);

                // 익명화 + 소프트 삭제
                var now = Instant.now();
                var purgeAt = now.plus(java.time.Duration.ofDays(30));
                user.anonymizeAndMarkDeleted(now, purgeAt);
                userRepo.save(user);
        }

        /** 영구삭제(배치용) */
        @Transactional
        public void hardDeleteUser(User user) {
                Long userId = user.getId();
                refreshTokenService.deleteByUser(user);
                commentLikeRepository.deleteByOwner(user);
                postLikeRepository.deleteByOwner(user);
                commentRepository.deleteByOwner(user);
                postRepository.deleteByOwner(user);
                gameDiaryRepository.deleteByUser(user);
                userStatsRepository.deleteById(userId);
                userRepo.delete(user);
        }
}
