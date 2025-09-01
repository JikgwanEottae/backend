package yagu.yagu.common.jwt;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.user.entity.User;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    long deleteByUser(User user);
}
