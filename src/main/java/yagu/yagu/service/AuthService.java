package yagu.yagu.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.entity.User;
import yagu.yagu.repository.UserRepository;
import yagu.yagu.security.JwtTokenProvider;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public User findOrCreateUser(String email, String nickname, User.AuthProvider provider, String providerId) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, nickname, provider, providerId));
    }

    private User createUser(String email, String nickname, User.AuthProvider provider, String providerId) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .provider(provider)
                .providerId(providerId)
                .build();
        return userRepository.save(user);
    }

    public Map<String, Object> createLoginResponse(User user) {
        String token = jwtTokenProvider.createToken(user.getEmail());
        log.info("jwt: {}", token);
        return Map.of(
                "token", token,
                "user", Map.of(
                        "email", user.getEmail(),
                        "nickname", user.getNickname(),
                        "provider", user.getProvider()
                )
        );
    }
}
