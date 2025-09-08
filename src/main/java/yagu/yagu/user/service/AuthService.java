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

import yagu.yagu.common.oauth.AppleTokenClient;
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
import java.util.Optional;

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
        private final AppleTokenClient appleTokenClient;

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
                        "nickname", user.getNickname(),
                        "profileCompleted", user.isProfileCompleted(),
                        "accessToken", accessToken,
                        "refreshToken", refresh.getToken()
                );
        }

        /**
         * 네이티브 로그인용: 매칭 우선순위
         * 1) provider + providerId
         * 2) 삭제되지 않은 사용자(by email)
         * 3) 30일 내 삭제된 사용자(by deletedOriginalEmail) → 복구(+provider 갱신)
         * 4) 신규 생성 (email 없을 때 대체 이메일 사용)
         */
        @Transactional
        public User findOrCreateByProvider(User.AuthProvider provider, String providerId,
                                           String emailOrNull, String nicknameFallback) {

                // 1) provider + providerId 우선
                Optional<User> byProv = userRepo.findByProviderAndProviderId(provider, providerId);
                if (byProv.isPresent()) {
                        User u = byProv.get();

                        if (u.getDeletedAt() != null) {
                                // 30일 내: 복구
                                if (u.getPurgeAt() == null || u.getPurgeAt().isAfter(Instant.now())) {
                                        // 복구용 이메일/닉네임 결정
                                        String restoreEmail = (emailOrNull != null && !emailOrNull.isBlank())
                                                ? emailOrNull
                                                : u.getDeletedOriginalEmail();

                                        // restoreEmail이 비었다면 안전한 대체 이메일 생성
                                        if (restoreEmail == null || restoreEmail.isBlank()) {
                                                restoreEmail = switch (provider) {
                                                        case APPLE -> providerId + "@apple.local";
                                                        case KAKAO -> "kakao_" + providerId + "@kakao.local";
                                                        default     -> providerId + "@local";
                                                };
                                        }

                                        String nick = (nicknameFallback != null && !nicknameFallback.isBlank())
                                                ? nicknameFallback
                                                : restoreEmail.split("@")[0];

                                        u.restoreFromDeletion(restoreEmail, nick);
                                        u.linkProvider(provider, providerId);
                                        return userRepo.save(u); // 복구 저장 후 반환
                                }
                                // 30일 경과: 반환하지 말고 아래 신규 생성 플로우로 진행 (fall-through)
                        } else {
                                // 삭제되지 않은 정상 계정이면 즉시 반환
                                return u; // ← 역슬래시 제거됨
                        }
                }

                // 2) 살아있는 동일 이메일 사용자
                if (emailOrNull != null && !emailOrNull.isBlank()) {
                        Optional<User> activeOpt = userRepo.findByEmailAndDeletedAtIsNull(emailOrNull);
                        if (activeOpt.isPresent()) {
                                return activeOpt.get();
                        }

                        // 3) 최근 30일 내 삭제된 사용자 복구
                        Optional<User> deletedOpt = userRepo.findByDeletedOriginalEmailAndDeletedAtIsNotNull(emailOrNull);
                        if (deletedOpt.isPresent()) {
                                User deletedUser = deletedOpt.get();
                                if (deletedUser.getPurgeAt() == null || deletedUser.getPurgeAt().isAfter(Instant.now())) {
                                        String nick = (nicknameFallback != null && !nicknameFallback.isBlank())
                                                ? nicknameFallback
                                                : emailOrNull.split("@")[0];
                                        deletedUser.restoreFromDeletion(emailOrNull, nick);
                                        deletedUser.linkProvider(provider, providerId);
                                        return userRepo.save(deletedUser);
                                }
                        }
                }

                // 4) 신규 생성 (email 없을 때 대체 이메일 사용)
                String email = emailOrNull;
                if (email == null || email.isBlank()) {
                        email = switch (provider) {
                                case APPLE -> providerId + "@apple.local";
                                case KAKAO -> "kakao_" + providerId + "@kakao.local";
                                default     -> providerId + "@local";
                        };
                }
                String nickname = (nicknameFallback != null && !nicknameFallback.isBlank())
                        ? nicknameFallback
                        : email.split("@")[0];

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

                if (user.getProvider() == User.AuthProvider.APPLE) {
                        String rt = user.getAppleRefreshToken();
                        if (rt != null && !rt.isBlank()) {
                                try { appleTokenClient.revokeRefreshToken(rt); } catch (Exception ignore) {}
                        }
                        user.updateAppleRefreshToken(null); // 보관값 비우기
                }
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


        /** 즉시탈퇴 */
        @Transactional
        public void immediateWithdraw(User user) {
                // 1) (애플만) 보관된 refresh 토큰 revoke 시도
                if (user.getProvider() == User.AuthProvider.APPLE) {
                        String rt = user.getAppleRefreshToken();
                        if (rt != null && !rt.isBlank()) {
                                try { appleTokenClient.revokeRefreshToken(rt); } catch (Exception ignore) {}
                        }
                        user.updateAppleRefreshToken(null);
                }

                // 2) 서버 보관 리프레시 토큰 전부 무효화
                refreshTokenService.deleteByUser(user);

                // 3) 모든 연관 데이터 포함 “즉시” 영구 삭제
                hardDeleteUser(user);
        }
}
