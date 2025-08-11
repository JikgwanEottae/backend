package yagu.yagu.diary.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import yagu.yagu.game.entity.KboGame;
import yagu.yagu.user.entity.User;

// no direct date/time fields in diary; referenced via KboGame

@Entity
@Table(name = "game_diary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private KboGame game;

    @Column(name = "favorite_team")
    private String favoriteTeam;

    @Column
    private String seat;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Result result;

    public enum Result {
        WIN, LOSS, DRAW
    }

    public GameDiary(User user,
            KboGame game,
            String favoriteTeam,
            Result result,
            String seat,
            String memo,
            String photoUrl) {
        this.user = user;
        this.game = game;
        this.favoriteTeam = favoriteTeam;
        this.result = result;
        this.seat = seat;
        this.memo = memo;
        this.photoUrl = photoUrl;
    }

    public static GameDiary of(User user,
            KboGame game,
            String favoriteTeam,
            Result result,
            String seat,
            String memo,
            String photoUrl) {
        return new GameDiary(user, game, favoriteTeam, result, seat, memo, photoUrl);
    }

    public void update(KboGame game,
            String favoriteTeam,
            Result result,
            String seat,
            String memo,
            String photoUrl) {
        if (game != null) {
            this.game = game;
        }
        this.favoriteTeam = favoriteTeam;
        this.result = result;
        this.seat = seat;
        this.memo = memo;
        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
    }

    // 명시적으로 이미지 URL을 설정(삭제 포함)하기 위한 메서드
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}