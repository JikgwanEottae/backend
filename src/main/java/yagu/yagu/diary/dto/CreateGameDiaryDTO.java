package yagu.yagu.diary.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateGameDiaryDTO {
    private LocalDate date;
    private String homeTeam;
    private String awayTeam;
    private String result;
    private String score;
    private String stadium;
    private String seat;
    private String memo;
    private String photoUrl;
}
