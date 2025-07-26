package yagu.yagu.game;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import yagu.yagu.game.entity.KboGame;
import yagu.yagu.game.repository.KboGameRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GameScheduleCrawler {

    private final KboGameRepository gameRepo;

    public GameScheduleCrawler(KboGameRepository gameRepo) {
        this.gameRepo = gameRepo;
    }

    /** 매일 새벽 3시에 자동 실행 */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void dailyUpdate() {
        crawlAndUpsert(LocalDate.now().getMonthValue());
    }

    /** 주어진 월(month) KBO 일정을 크롤링 & upsert */
    public void crawlAndUpsert(int month) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments(
                "--headless",
                "--no-sandbox",
                "--disable-gpu",
                "--window-size=1920,1080"
        );
        WebDriver driver = new ChromeDriver(opts);

        try {
            // 1) 페이지 열기 및 월 선택
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            new Select(wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("ddlMonth"))
            )).selectByValue(String.format("%02d", month));

            // 2) 표 로드 & tbody rows
            WebElement table = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.className("tbl-type06"))
            );
            WebElement tbody = table.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));

            LocalDate currentDate = null;
            for (WebElement row : rows) {
                if (row.getText().contains("데이터가 없습니다")) continue;

                // — 날짜 갱신 (td.day 있을 때만)
                List<WebElement> dayTd = row.findElements(By.cssSelector("td.day"));
                if (!dayTd.isEmpty()) {
                    String d = dayTd.get(0).getText().trim().substring(0,5);
                    int m  = Integer.parseInt(d.substring(0,2));
                    int dd = Integer.parseInt(d.substring(3,5));
                    currentDate = LocalDate.of(LocalDate.now().getYear(), m, dd);
                }

                // — 시간
                String timeStr = row.findElement(By.cssSelector("td.time b"))
                        .getText().trim();
                LocalTime time = LocalTime.parse(timeStr);

                // — 경기 (away / home / score)
                WebElement play = row.findElement(By.cssSelector("td.play"));
                List<WebElement> spans = play.findElements(By.tagName("span"));
                String awayTeam = spans.get(0).getText().trim();
                String homeTeam = spans.get(spans.size() - 1).getText().trim();

                Integer awayScore = null, homeScore = null;
                Status status = Status.SCHEDULED;
                List<WebElement> ems = play.findElements(By.tagName("em"));
                if (!ems.isEmpty()) {
                    String scoreTxt = ems.get(0).getText().trim();
                    Matcher sc = Pattern.compile("(\\d+)vs(\\d+)").matcher(scoreTxt);
                    if (sc.find()) {
                        awayScore = Integer.valueOf(sc.group(1));
                        homeScore = Integer.valueOf(sc.group(2));
                        status = Status.PLAYED;
                    }
                }

                // — relay 셀 기준으로 다음 형제들
                WebElement relayCell = row.findElement(By.cssSelector("td.relay"));
                String tvChannel = relayCell
                        .findElement(By.xpath("following-sibling::td[2]"))
                        .getText().trim();
                String stadium   = relayCell
                        .findElement(By.xpath("following-sibling::td[4]"))
                        .getText().trim();
                String note      = relayCell
                        .findElement(By.xpath("following-sibling::td[5]"))
                        .getText().trim();
                if (note.isEmpty()) note = "-";

                // — upsert (if-else 방식)
                Optional<KboGame> opt = gameRepo
                        .findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
                                currentDate, time, homeTeam, awayTeam
                        );
                KboGame game;
                if (opt.isPresent()) {
                    game = opt.get();
                } else {
                    game = new KboGame();
                    game.setGameDate(currentDate);
                    game.setGameTime(time);
                    game.setHomeTeam(homeTeam);
                    game.setAwayTeam(awayTeam);
                }

                // — 공통 필드
                game.setStatus(status);
                game.setHomeScore(homeScore);
                game.setAwayScore(awayScore);
                game.setTvChannel(tvChannel);
                game.setStadium(stadium);
                game.setNote(note);

                gameRepo.save(game);
            }
        } finally {
            driver.quit();
        }
    }
}