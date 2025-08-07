package yagu.yagu.diary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStats {

    @Id
    private Long userId;

    private int winCount;
    private int lossCount;
    private int drawCount;

    private double winRate;


    public UserStats(Long userId) {
        this.userId    = userId;
        this.winCount  = 0;
        this.lossCount = 0;
        this.drawCount = 0;
        this.winRate   = 0.0;
    }

    public enum Result { WIN, LOSS, DRAW }

    public void updateOnNew(Result r) {
        switch (r) {
            case WIN  -> winCount++;
            case LOSS -> lossCount++;
            case DRAW -> drawCount++;
        }
        recalcRate();
    }

    public void updateOnDelete(Result r) {
        switch (r) {
            case WIN  -> winCount--;
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