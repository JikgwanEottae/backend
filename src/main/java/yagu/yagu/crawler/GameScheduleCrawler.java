package yagu.yagu.crawler;

import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
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
import yagu.yagu.entity.KboGame;
import yagu.yagu.entity.Status;
import yagu.yagu.repository.KboGameRepository;

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


    /** 매일 새벽 3시에 자동 실행  */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void dailyUpdate() {
        int month = LocalDate.now().getMonthValue();
        crawlAndUpsert(month);
    }

    /**
     * 주어진 월(month) KBO 일정을 크롤링
     */
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
            // 1) 페이지 열기
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 2) 월 선택
            Select select = new Select(
                    wait.until(
                            ExpectedConditions.elementToBeClickable(By.id("ddlMonth"))
                    )
            );
            select.selectByValue(String.format("%02d", month));

            // 3) 표 로드 대기
            WebElement table = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.className("tbl-type06"))
            );

            // 4) 각 행 순회
            List<WebElement> rows = table.findElements(By.tagName("tr"));
            LocalDate currentDate = null;

            for (WebElement row : rows) {
                String text = row.getText().trim();

                if (text.isEmpty()
                        || text.contains("데이터가 없습니다")
                        || text.startsWith("날짜 ")) {
                    continue;
                }

                String[] cols = text.split("\\s+");
                int idx = 0;

                // 날짜 갱신
                if (cols[0].endsWith(")")) {
                    String mmdd = cols[0].substring(0,5);
                    int m = Integer.parseInt(mmdd.substring(0,2));
                    int d = Integer.parseInt(mmdd.substring(3,5));
                    currentDate = LocalDate.of(LocalDate.now().getYear(), m, d);
                    idx = 1;
                }


                String timeStr = cols[idx++];
                String match   = cols[idx++];
                String tv      = cols[idx++];
                String stadium = cols[idx++];
                String note    = idx < cols.length ? cols[idx] : "-";

                LocalTime time = LocalTime.parse(timeStr);

                // 팀·스코어 파싱
                Matcher m = Pattern.compile("(.+?)(\\d+)vs(\\d+)(.+)")
                        .matcher(match);
                String homeTeam, awayTeam;
                Integer homeScore = null, awayScore = null;
                Status status;

                if (m.matches()) {
                    homeTeam  = m.group(1);
                    homeScore = Integer.valueOf(m.group(2));
                    awayScore = Integer.valueOf(m.group(3));
                    awayTeam  = m.group(4);
                    status    = Status.PLAYED;
                } else {
                    String[] t = match.split("vs");
                    homeTeam = t[0];
                    awayTeam = t[1];
                    status   = Status.SCHEDULED;
                }

                // 5)  기존 행 조회
                Optional<KboGame> existing = gameRepo
                        .findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
                                currentDate, time, homeTeam, awayTeam
                        );

                // 6) 엔티티 생성 or 업데이트
                KboGame game;
                if (existing.isPresent()) {
                    game = existing.get();
                } else {
                    game = new KboGame();
                    game.setGameDate(currentDate);
                    game.setGameTime(time);
                    game.setHomeTeam(homeTeam);
                    game.setAwayTeam(awayTeam);
                }

                // 공통 필드 세팅
                game.setTvChannel(tv);
                game.setStadium(stadium);
                game.setNote(note);
                game.setHomeScore(homeScore);
                game.setAwayScore(awayScore);
                game.setStatus(status);

                // 7) 저장
                gameRepo.save(game);
            }
        } finally {
            driver.quit();
        }
    }
}
