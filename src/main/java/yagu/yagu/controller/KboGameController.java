package yagu.yagu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yagu.yagu.entity.KboGame;
import yagu.yagu.repository.KboGameRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
public class KboGameController {
    private final KboGameRepository repo;

    public KboGameController(KboGameRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/games")
    public List<KboGame> getByTeam(@RequestParam String team) {
        return repo.findByHomeTeamContainingOrAwayTeamContainingOrderByGameDateAscGameTimeAsc(team, team);
    }
}
