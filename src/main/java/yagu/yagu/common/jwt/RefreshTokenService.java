package yagu.yagu.common.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.user.entity.User;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;
    private final JwtConfig config;

    public RefreshToken createRefreshToken(User user) {
        String newToken = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusMillis(config.getRefreshExpiration());
        RefreshToken refreshToken = RefreshToken.of(user, newToken, expiry);
        return repo.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            repo.delete(token);
            throw new RuntimeException("Expired refresh token: " + token.getToken());
        }
        return token;
    }


    public RefreshToken findByToken(String token) {
        return repo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    @Transactional
    public void deleteByUser(User user) {
        repo.deleteByUserId(user.getId());
    }
}
