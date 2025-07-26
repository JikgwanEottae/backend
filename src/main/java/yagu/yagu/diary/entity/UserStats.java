package yagu.yagu.diary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStats {
    @Id
    private Long userId;

    private int winCount;
    private int lossCount;
    private int drawCount;

    @Column
    private double winRate;

    public void updateOnNew(Result r) {
        switch (r) {
            case WIN -> winCount++;
            case LOSS -> lossCount++;
            case DRAW -> drawCount++;
        }
        recalcRate();
    }

    public void updateOnChange(Result oldR, Result newR) {
        if (oldR == newR) return;
        updateOnDelete(oldR);
        updateOnNew(newR);
    }

    public void updateOnDelete(Result r) {
        switch (r) {
            case WIN -> winCount--;
            case LOSS -> lossCount--;
            case DRAW -> drawCount--;
        }
        recalcRate();
    }

    private void recalcRate() {
        int total = winCount + lossCount + drawCount;
        this.winRate = total == 0 ? 0.0 : (double) winCount / total * 100.0;
    }

    public enum Result {
        WIN, LOSS, DRAW
    }
}