package yagu.yagu.diary.entity;

import jakarta.persistence.*;
import lombok.*;
import yagu.yagu.user.entity.User;

import java.time.LocalDate;

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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Result result;        // WIN, LOSS

    @Column(nullable = false)
    private String score;         // ex) "4-3"

    @Column(nullable = false)
    private String stadium;

    @Column
    private String seat;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "photo_url")
    private String photoUrl;

    public enum Result { WIN, LOSS }
}