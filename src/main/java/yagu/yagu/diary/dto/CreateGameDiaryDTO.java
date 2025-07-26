package yagu.yagu.diary.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateGameDiaryDTO {
    // 프론트가 그대로 보내는 게임 정보
    @Schema(type = "string", pattern = "yyyy-MM-dd", example = "2025-07-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate gameDate;

    @Schema(type = "string", pattern = "HH:mm:ss", example = "18:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime gameTime;

    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String tvChannel;
    private String note;
    private Integer homeScore;
    private Integer awayScore;
    private String status;
    private String winTeam;

    // 프론트가 추가 입력하는 일기 정보
    private String favoriteTeam; // 사용자가 응원한 팀
    private String seat;
    private String memo;
    private String photoUrl;
}
