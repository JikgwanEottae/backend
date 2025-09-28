package yagu.yagu.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.diary.entity.UserStats;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UserStats us where us.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
