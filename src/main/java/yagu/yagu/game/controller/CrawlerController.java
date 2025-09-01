package yagu.yagu.game.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.game.GameScheduleCrawler;

import java.time.YearMonth;

@RestController
@RequestMapping("/api")
public class CrawlerController {

    private final GameScheduleCrawler crawler;

    public CrawlerController(GameScheduleCrawler crawler) {
        this.crawler = crawler;
    }

    /** 연/월 크롤링 (예: /api/month/2025/10) */
    @GetMapping("/month/{year}/{month}")
    public ResponseEntity<String> crawlYearMonth(@PathVariable int year, @PathVariable int month) {
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().body("month는 1~12 사이여야 합니다.");
        }
        crawler.crawlAndUpsert(year, month); // 정규 → 포스트시즌 순서로 둘 다 파싱
        return ResponseEntity.ok(String.format("크롤링 완료: %d-%02d — 정규+포스트시즌", year, month));
    }

    /**
     * 범위 크롤링 (예: /api/month/range?from=202409&to=202411)
     */
    @GetMapping("/month/range")
    public ResponseEntity<String> crawlRange(@RequestParam String from, @RequestParam String to) {
        if (from == null || to == null || from.length() != 6 || to.length() != 6) {
            return ResponseEntity.badRequest().body("from, to는 yyyymm 형식이어야 합니다. 예: from=202409&to=202411");
        }
        int fy = Integer.parseInt(from.substring(0, 4));
        int fm = Integer.parseInt(from.substring(4, 6));
        int ty = Integer.parseInt(to.substring(0, 4));
        int tm = Integer.parseInt(to.substring(4, 6));
        if (fm < 1 || fm > 12 || tm < 1 || tm > 12) {
            return ResponseEntity.badRequest().body("월은 1~12 사이여야 합니다.");
        }

        YearMonth start = YearMonth.of(fy, fm);
        YearMonth end = YearMonth.of(ty, tm);
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body("to는 from보다 같거나 이후여야 합니다.");
        }

        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            crawler.crawlAndUpsert(ym.getYear(), ym.getMonthValue());
        }

        return ResponseEntity.ok(String.format("크롤링 완료: %s ~ %s — 정규+포스트시즌",
                start, end));
    }
}