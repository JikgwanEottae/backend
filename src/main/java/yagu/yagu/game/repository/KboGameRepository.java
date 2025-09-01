package yagu.yagu.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.game.entity.KboGame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface KboGameRepository extends JpaRepository<KboGame, Long> {
    Optional<KboGame> findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
            LocalDate date, LocalTime time, String homeTeam, String awayTeam);

    // 특정 날짜의 모든 경기
    List<KboGame> findByGameDate(LocalDate gameDate);

    // 날짜 범위 내 경기 (월별 조회용)
    List<KboGame> findByGameDateBetween(LocalDate start, LocalDate end);

}
