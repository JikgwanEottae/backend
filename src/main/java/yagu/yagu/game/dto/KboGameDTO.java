package yagu.yagu.game.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import yagu.yagu.game.entity.KboGame;


import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class KboGameDTO {
    private Long id;
    private LocalDate gameDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime gameTime;

    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String note;
    private Integer homeScore;
    private Integer awayScore;
    private KboGame.Status status;
    private String winTeam;


    public KboGameDTO(KboGame g) {
        this(
                g.getId(),
                g.getGameDate(),
                g.getGameTime(),
                g.getHomeTeam(),
                g.getAwayTeam(),
                g.getStadium(),
                g.getNote(),
                g.getHomeScore(),
                g.getAwayScore(),
                g.getStatus(),
                g.getWinTeam()
        );
    }
}