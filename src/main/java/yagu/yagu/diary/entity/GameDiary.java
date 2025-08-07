package yagu.yagu.diary.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import yagu.yagu.user.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "game_diary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameDiary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;


    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "game_time")
    private LocalTime gameTime;

    @Column(name = "home_team", nullable = false)
    private String homeTeam;

    @Column(name = "away_team", nullable = false)
    private String awayTeam;

    @Column(name = "home_score", nullable = false)
    private Integer homeScore;

    @Column(name = "away_score", nullable = false)
    private Integer awayScore;

    @Column(name = "win_team")
    private String winTeam;

    @Column(name = "favorite_team")
    private String favoriteTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Result result;

    @Column(nullable = false)
    private String stadium;

    @Column
    private String seat;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "photo_url")
    private String photoUrl;

    public enum Result { WIN, LOSS, DRAW }

    public GameDiary(User user,
                     LocalDate gameDate,
                     LocalTime gameTime,
                     String homeTeam,
                     String awayTeam,
                     Integer homeScore,
                     Integer awayScore,
                     String winTeam,
                     String favoriteTeam,
                     Result result,
                     String stadium,
                     String seat,
                     String memo,
                     String photoUrl) {
        this.user         = user;
        this.gameDate     = gameDate;
        this.gameTime     = gameTime;
        this.homeTeam     = homeTeam;
        this.awayTeam     = awayTeam;
        this.homeScore    = homeScore;
        this.awayScore    = awayScore;
        this.winTeam      = winTeam;
        this.favoriteTeam = favoriteTeam;
        this.result       = result;
        this.stadium      = stadium;
        this.seat         = seat;
        this.memo         = memo;
        this.photoUrl     = photoUrl;
    }


    public static GameDiary of(User user,
                               LocalDate gameDate,
                               LocalTime gameTime,
                               String homeTeam,
                               String awayTeam,
                               Integer homeScore,
                               Integer awayScore,
                               String winTeam,
                               String favoriteTeam,
                               Result result,
                               String stadium,
                               String seat,
                               String memo,
                               String photoUrl) {
        return new GameDiary(user, gameDate, gameTime,
                homeTeam, awayTeam,
                homeScore, awayScore,
                winTeam, favoriteTeam,
                result, stadium, seat, memo, photoUrl);
    }


    public void update(LocalDate gameDate,
                       LocalTime gameTime,
                       String homeTeam,
                       String awayTeam,
                       Integer homeScore,
                       Integer awayScore,
                       String winTeam,
                       String favoriteTeam,
                       Result result,
                       String stadium,
                       String seat,
                       String memo,
                       String photoUrl) {
        this.gameDate     = gameDate;
        this.gameTime     = gameTime;
        this.homeTeam     = homeTeam;
        this.awayTeam     = awayTeam;
        this.homeScore    = homeScore;
        this.awayScore    = awayScore;
        this.winTeam      = winTeam;
        this.favoriteTeam = favoriteTeam;
        this.result       = result;
        this.stadium      = stadium;
        this.seat         = seat;
        this.memo         = memo;
        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
    }
}