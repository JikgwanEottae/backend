package yagu.yagu.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.game.entity.GameRaw;
import yagu.yagu.game.entity.GameRaw.Status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface GameRawRepository extends JpaRepository<GameRaw, Long> {
    List<GameRaw> findByStatus(Status status);

    List<GameRaw> findByStatusIn(List<Status> statuses);

    Optional<GameRaw> findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
            LocalDate gameDate,
            LocalTime gameTime,
            String homeTeam,
            String awayTeam
    );

    List<GameRaw> findByStatusAndGameDateAfter(Status status, LocalDate date);

    List<GameRaw> findByGameDateBetween(LocalDate start, LocalDate end);
}

