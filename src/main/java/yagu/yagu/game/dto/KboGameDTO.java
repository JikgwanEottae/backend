package yagu.yagu.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import yagu.yagu.game.entity.KboGame;
import yagu.yagu.game.entity.Status;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class KboGameDTO {
    private Long id;
    private LocalDate gameDate;
    private LocalTime gameTime;
    private String homeTeam;
    private String awayTeam;
    private String stadium;
    private String tvChannel;
    private String note;
    private Integer homeScore;
    private Integer awayScore;
    private Status status;

    public KboGameDTO(KboGame g) {
        this(
                g.getId(),
                g.getGameDate(),
                g.getGameTime(),
                g.getHomeTeam(),
                g.getAwayTeam(),
                g.getStadium(),
                g.getTvChannel(),
                g.getNote(),
                g.getHomeScore(),
                g.getAwayScore(),
                g.getStatus()
        );
    }
}