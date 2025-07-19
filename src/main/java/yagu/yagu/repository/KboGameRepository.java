package yagu.yagu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.entity.KboGame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface KboGameRepository extends JpaRepository<KboGame, Long> {
    Optional<KboGame> findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
            LocalDate date, LocalTime time, String homeTeam, String awayTeam);

    List<KboGame> findByHomeTeamContainingOrAwayTeamContainingOrderByGameDateAscGameTimeAsc(
            String home, String away);
}
