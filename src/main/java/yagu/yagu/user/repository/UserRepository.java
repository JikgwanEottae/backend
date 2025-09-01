package yagu.yagu.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    java.util.List<User> findByDeletedAtIsNotNullAndPurgeAtBefore(java.time.Instant time);

    Optional<User> findByDeletedOriginalEmailAndDeletedAtIsNotNull(String deletedOriginalEmail);
}
