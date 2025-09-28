package yagu.yagu.diary.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameDiaryDetailDTO {
    private Long diaryId;
    private LocalDate date;
    private LocalTime gameTime;
    private int homeScore;
    private int awayScore;
    private String winTeam;
    private String favoriteTeam;
    private String title;

    private String homeTeam;
    private String awayTeam;
    private String result;
    private String stadium;
    private String seat;
    private String memo;
    private String photoUrl;
}
