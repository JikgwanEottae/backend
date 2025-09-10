package yagu.yagu.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    java.util.List<User> findByDeletedAtIsNotNullAndPurgeAtBefore(java.time.Instant time);

    Optional<User> findByDeletedOriginalEmailAndDeletedAtIsNotNull(String deletedOriginalEmail);

    Optional<User> findByProviderAndProviderId(User.AuthProvider provider, String providerId);

    @Modifying
    @Query("update User u set u.appleRefreshToken = :rt where u.id = :id")
    int updateAppleRefreshTokenById(@Param("id") Long id, @Param("rt") String rt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.nickname = :nick, u.profileCompleted = true where u.id = :id")
    int completeProfileById(@Param("id") Long id, @Param("nick") String nick);

}
