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
import yagu.yagu.game.entity.Status;
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

    /** 매일 새벽 3시 (올해, 현재 월) */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void dailyUpdate() {
        LocalDate now = LocalDate.now();
        crawlAndUpsert(now.getYear(), now.getMonthValue());
    }

    /** 올해 기준 특정 월 (호환용) */
    public void crawlAndUpsert(int month) {
        crawlAndUpsert(LocalDate.now().getYear(), month);
    }

    /** [핵심] 연/월 크롤링: 정규 → 포스트시즌 둘 다 처리 */
    public void crawlAndUpsert(int year, int month) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless","--no-sandbox","--disable-gpu","--window-size=1920,1080");
        WebDriver driver = new ChromeDriver(opts);

        try {
            driver.get("https://www.koreabaseball.com/Schedule/Schedule.aspx");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            // 연/월 선택
            new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlYear"))))
                    .selectByValue(String.valueOf(year));
            new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("ddlMonth"))))
                    .selectByValue(String.format("%02d", month));

            // 1) 정규시즌
            selectCategoryLoosely(wait, "정규시즌");
            int savedRegular = parseTableAndUpsert(driver, wait, year);

            // 2) 포스트시즌
            selectCategoryLoosely(wait, "포스트시즌");
            int savedPost = parseTableAndUpsert(driver, wait, year);

            System.out.printf("[KBO] %d-%02d 저장: 정규 %d건, 포스트시즌 %d건%n",
                    year, month, savedRegular, savedPost);

        } finally {
            driver.quit();
        }
    }

    /** 정규/포스트시즌 셀렉트 (부분 텍스트 매칭) */
    private void selectCategoryLoosely(WebDriverWait wait, String containsKo) {
        WebElement selectEl = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//select[option[contains(.,'정규시즌') or contains(.,'포스트시즌')]]")
        ));
        // 옵션 원문 텍스트 가져와서 선택 (가끔 문구가 살짝 달라져서)
        WebElement opt = selectEl.findElement(By.xpath(".//option[contains(normalize-space(.),'" + containsKo + "')]"));
        new Select(selectEl).selectByVisibleText(opt.getText());

        // 테이블 리로드 대기
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("tbl-type06")));
    }

    /** 표 파싱 & upsert: 실제 경기 행만 저장, 저장 건수 반환 */
    private int parseTableAndUpsert(WebDriver driver, WebDriverWait wait, int year) {
        WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("tbl-type06")));
        WebElement tbody = table.findElement(By.tagName("tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));

        LocalDate currentDate = null;
        int saved = 0;

        for (WebElement row : rows) {
            // “데이터가 없습니다” 행 스킵
            if (row.getText().contains("데이터가 없습니다")) continue;

            // 날짜(rowspan)
            List<WebElement> dayTd = row.findElements(By.cssSelector("td.day"));
            if (!dayTd.isEmpty()) {
                String mmdd = dayTd.get(0).getText().trim();
                if (mmdd.length() >= 5) {
                    int m = Integer.parseInt(mmdd.substring(0, 2));
                    int d = Integer.parseInt(mmdd.substring(3, 5));
                    currentDate = LocalDate.of(year, m, d);
                }
            }
            if (currentDate == null) continue;

            // 비경기 행 가드 1: time b가 없으면 스킵
            List<WebElement> timeB = row.findElements(By.cssSelector("td.time b"));
            if (timeB.isEmpty()) continue;

            // 시간 파싱 (미정/TBD/빈칸 방어)
            LocalTime time = safeParseTime(timeB.get(0).getText());
            if (time == null) continue;

            // 팀/스코어
            WebElement play = row.findElement(By.cssSelector("td.play"));
            List<WebElement> spans = play.findElements(By.tagName("span"));
            if (spans.size() < 2) continue; // 팀 스팬 2개 미만 = 경기 아님
            String awayTeam = spans.get(0).getText().trim();
            String homeTeam = spans.get(spans.size() - 1).getText().trim();

            Integer awayScore = null, homeScore = null;
            Status status = Status.SCHEDULED;
            List<WebElement> ems = play.findElements(By.tagName("em"));
            if (!ems.isEmpty()) {
                String scoreTxt = ems.get(0).getText().trim();
                Matcher sc = Pattern.compile("(\\d+)\\s*vs\\s*(\\d+)").matcher(scoreTxt);
                if (sc.find()) {
                    awayScore = parseIntSafe(sc.group(1));
                    homeScore = parseIntSafe(sc.group(2));
                    status = Status.PLAYED;
                }
            }

            // TV/구장/비고
            WebElement relayCell = row.findElement(By.cssSelector("td.relay"));
            String tvChannel = relayCell.findElement(By.xpath("following-sibling::td[2]")).getText().trim();
            String stadium   = relayCell.findElement(By.xpath("following-sibling::td[4]")).getText().trim();
            String rawNote   = relayCell.findElement(By.xpath("following-sibling::td[5]")).getText().trim();
            String note = (rawNote.isEmpty() || "-".equals(rawNote)) ? null : rawNote;

            // upsert
            Optional<KboGame> opt = gameRepo.findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
                    currentDate, time, homeTeam, awayTeam
            );
            KboGame game = opt.orElseGet(KboGame::new);
            if (game.getId() == null) {
                game.setGameDate(currentDate);
                game.setGameTime(time);
                game.setHomeTeam(homeTeam);
                game.setAwayTeam(awayTeam);
            }
            game.setStatus(status);
            game.setHomeScore(homeScore);
            game.setAwayScore(awayScore);
            game.setTvChannel(tvChannel);
            game.setStadium(stadium);
            game.setNote(note);

            String winTeam = null;
            if (awayScore != null && homeScore != null) {
                if (homeScore > awayScore) winTeam = homeTeam;
                else if (homeScore < awayScore) winTeam = awayTeam;
                else winTeam = "무승부";
            }
            game.setWinTeam(winTeam);

            gameRepo.save(game);
            saved++;
        }
        return saved;
    }

    // ===== util =====

    private LocalTime safeParseTime(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || "-".equals(s) || s.equalsIgnoreCase("TBD") || s.contains("추후")) return null;
        s = s.replaceAll("[^0-9:]", ""); // "18:30 (예정)" 같은 변형 방지
        try { return LocalTime.parse(s); } catch (Exception e) { return null; }
    }

    private Integer parseIntSafe(String n) {
        try { return Integer.valueOf(n.trim()); } catch (Exception e) { return null; }
    }
}