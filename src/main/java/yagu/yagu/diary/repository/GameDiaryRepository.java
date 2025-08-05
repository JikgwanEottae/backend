package yagu.yagu.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.diary.entity.GameDiary;
import yagu.yagu.user.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface GameDiaryRepository extends JpaRepository<GameDiary, Long> {
    List<GameDiary> findAllByUserIdAndGameDateBetween(Long userId, LocalDate start, LocalDate end);

    List<GameDiary> findAllByUserIdOrderByGameDateDesc(Long userId);

    long deleteByUser(User user);

    List<GameDiary> findAllByUserIdAndGameDateBetweenOrderByGameDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}