package yagu.yagu.diary.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameDiaryCalendarDTO {
    private Long diaryId;
    private LocalDate date;
    private String result;
    private String score;
}
