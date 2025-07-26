package yagu.yagu.game.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Setter
@NoArgsConstructor
public class KboGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "tv_channel", length = 20)
    private String tvChannel;

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
}
