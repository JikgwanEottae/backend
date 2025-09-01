package yagu.yagu.diary.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {
    private Long userId;
    private int winCount;
    private int lossCount;
    private int drawCount;
    private double winRate;
}