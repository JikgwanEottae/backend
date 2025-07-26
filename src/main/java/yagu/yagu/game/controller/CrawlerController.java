package yagu.yagu.game.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.game.GameScheduleCrawler;

@RestController
@RequestMapping("/api")
public class CrawlerController {
    private final GameScheduleCrawler crawler;

    public CrawlerController(GameScheduleCrawler crawler) {
        this.crawler = crawler;
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
