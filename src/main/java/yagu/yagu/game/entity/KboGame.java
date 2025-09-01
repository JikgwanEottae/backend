package yagu.yagu.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "games",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"game_date","game_time","home_team","away_team"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KboGame {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "game_time", nullable = false)
    private LocalTime gameTime;

    @Column(name = "home_team", length = 50, nullable = false)
    private String homeTeam;

    @Column(name = "away_team", length = 50, nullable = false)
    private String awayTeam;

    @Column(length = 50)
    private String stadium;

    @Column(length = 100)
    private String note;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "win_team", length = 50)
    private String winTeam;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        SCHEDULED,
        PLAYED,
        CANCELED
    }


    public KboGame(LocalDate gameDate,
                   LocalTime gameTime,
                   String homeTeam,
                   String awayTeam) {
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    public static KboGame of(LocalDate gameDate,
                             LocalTime gameTime,
                             String homeTeam,
                             String awayTeam) {
        return new KboGame(gameDate, gameTime, homeTeam, awayTeam);
    }

    public void applyScrape(Status status,
                            Integer homeScore,
                            Integer awayScore,
                            String stadium,
                            String note,
                            String winTeam) {
        this.status    = status;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.stadium   = stadium;
        this.note      = note;
        this.winTeam   = winTeam;
    }
}
