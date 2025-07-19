package yagu.yagu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.crawler.GameScheduleCrawler;
import yagu.yagu.entity.KboGame;
import yagu.yagu.repository.KboGameRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
public class KboGameController {
    private final KboGameRepository repo;
    private final GameScheduleCrawler crawler;

    public KboGameController(KboGameRepository repo, GameScheduleCrawler crawler) {
        this.repo = repo;
        this.crawler = crawler;
    }

    @GetMapping("/games")
    public List<KboGame> getByTeam(@RequestParam String team) {
        return repo.findByHomeTeamContainingOrAwayTeamContainingOrderByGameDateAscGameTimeAsc(team, team);
    }

    /**
    month 크롤링
     */
    @GetMapping("/month/{month}")
    public ResponseEntity<String> crawlMonth(@PathVariable int month) {
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest()
                    .body("month는 1~12 사이");
        }
        crawler.crawlAndUpsert(month);
        return ResponseEntity.ok("크롤링 완료: " + month + "월");
    }
}
