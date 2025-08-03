package yagu.yagu.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;
import yagu.yagu.common.jwt.JwtTokenProvider;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final JwtTokenProvider jwtProvider;

    /** 기존 회원 조회 or 신규 생성 (profileCompleted=false) */
    @Transactional
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

    /** 로그인 완료 후 JWT + 유저 info 반환 */
    public Map<String, Object> createLoginResponse(User user) {
        String token = jwtProvider.createToken(user.getEmail());
        return Map.of(
                "token", token,
                "user", Map.of(
                        "email", user.getEmail(),
                        "nickname", user.getNickname(),
                        "provider", user.getProvider(),
                        "profileCompleted", user.isProfileCompleted()
                )
        );
    }
}
