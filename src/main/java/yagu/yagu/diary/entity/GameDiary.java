package yagu.yagu.diary.entity;

import jakarta.persistence.*;
import lombok.*;
import yagu.yagu.user.entity.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "game_diary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameDiary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "home_team", nullable = false)
    private String homeTeam;

    @Column(name = "away_team", nullable = false)
    private String awayTeam;

    @Column(name = "game_time")
    private LocalTime gameTime;

    @Column(name = "tv_channel")
    private String tvChannel;

    @Column(name = "home_score", nullable = false)
    private Integer homeScore;

    @Column(name = "away_score", nullable = false)
    private Integer awayScore;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "win_team")
    private String winTeam;

    @Column(name = "favorite_team")
    private String favoriteTeam;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Result result;        // WIN, LOSS, DRAW

    @Column(nullable = false)
    private String stadium;

    @Column
    private String seat;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "photo_url")
    private String photoUrl;

    public enum Result { WIN, LOSS , DRAW}
}