package yagu.yagu.diary.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import yagu.yagu.user.entity.User;

@Entity
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStats {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @MapsId
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    private int winCount;
    private int lossCount;
    private int drawCount;

    private double winRate;

    public UserStats(User user) {
        this.user = user;
        // userId는 @MapsId에 의해 자동으로 매핑됨
        this.winCount = 0;
        this.lossCount = 0;
        this.drawCount = 0;
        this.winRate = 0.0;
    }

    public enum Result {
        WIN, LOSS, DRAW
    }

    public void updateOnNew(Result r) {
        switch (r) {
            case WIN -> winCount++;
            case LOSS -> lossCount++;
            case DRAW -> drawCount++;
        }
        recalcRate();
    }

    public void updateOnDelete(Result r) {
        switch (r) {
            case WIN -> winCount--;
            case LOSS -> lossCount--;
            case DRAW -> drawCount--;
        }
        recalcRate();
    }

    public void updateOnChange(Result oldR, Result newR) {
        if (oldR != newR) {
            updateOnDelete(oldR);
            updateOnNew(newR);
        }
    }

    private void recalcRate() {
        int total = winCount + lossCount + drawCount;
        this.winRate = total == 0 ? 0.0 : (double) winCount / total * 100.0;
    }
}