package yagu.yagu.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.diary.entity.GameDiary;
import yagu.yagu.user.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface GameDiaryRepository extends JpaRepository<GameDiary, Long> {
    List<GameDiary> findAllByUserIdOrderByGame_GameDateDesc(Long userId);

    long deleteByUser(User user);

    List<GameDiary> findAllByUserIdAndGame_GameDateBetweenOrderByGame_GameDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate);
}