package yagu.yagu.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.diary.entity.UserStats;

public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
}
