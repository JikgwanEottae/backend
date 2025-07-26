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

    @Column
    private double winRate;

    public void updateOnNew(Result r) {
        if (r == Result.WIN) winCount++;
        else lossCount++;
        recalcRate();
    }

    public void updateOnChange(Result oldR, Result newR) {
        if (oldR == newR) return;
        if (oldR == Result.WIN) winCount--;
        else lossCount--;
        updateOnNew(newR);
    }

    public void updateOnDelete(Result r) {
        if (r == Result.WIN) winCount--;
        else lossCount--;
        recalcRate();
    }

    private void recalcRate() {
        int total = winCount + lossCount;
        this.winRate = total == 0 ? 0.0 : (double) winCount / total * 100.0;
    }

    public enum Result { WIN, LOSS }
}