package yagu.yagu.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import yagu.yagu.game.entity.GameRaw;
import yagu.yagu.game.entity.KboGame;
import yagu.yagu.game.entity.KboGame.Status;
import yagu.yagu.game.repository.GameRawRepository;
import yagu.yagu.game.repository.KboGameRepository;
import yagu.yagu.game.service.CrawlCheckpointService;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class GameScheduleCrawler {

    private final KboGameRepository gameRepo;
    private final GameRawRepository gameRawRepo;
    private final CrawlCheckpointService checkpointService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 매일 00:15 크롤링 (올해, 현재 월) */
    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Seoul")
    public void dailyUpdate() {
        try {
            LocalDate today = LocalDate.now();
            
            // 1. 크롤링 + 파싱 (crawlTableAndSave에서 함께 수행)
            crawlGames(today.getYear(), today.getMonthValue());
            
            // 2. 파싱 (실패한 건 재시도용, 선택적)
            parseGames();
            
            // 3. 확정
            confirmGames();
            
            // 4. 체크포인트 업데이트
            checkpointService.updateLastSuccessDate("KBO_CRAWL_DAILY", today);
            
            System.out.printf("[KBO Crawler] Daily update completed: %s%n", today);
            
        } catch (Exception e) {
            System.err.println("[KBO Crawler] Error: " + e.getMessage());
            e.printStackTrace();
            
            // 안전장치: 실패 시 기존 방식으로 폴백
            try {
                System.err.println("[KBO Crawler] Fallback to legacy method...");
                LocalDate now = LocalDate.now();
                crawlAndUpsert(now.getYear(), now.getMonthValue());
            } catch (Exception fallbackError) {
                System.err.println("[KBO Crawler] Fallback also failed: " + fallbackError.getMessage());
            }
        }
    }

    /** @deprecated 새로운 플로우(crawlGames → confirmGames) 사용 */
    @Deprecated
    /** [핵심] 연/월 크롤링: 정규 → 포스트시즌 둘 다 처리 */
    public void crawlAndUpsert(int year, int month) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new","--no-sandbox","--disable-dev-shm-usage","--disable-gpu","--window-size=1920,1080");

        WebDriver driver;
        String remote = System.getenv("SELENIUM_URL");
        if (remote != null && !remote.isBlank()) {
            try {
                driver = new RemoteWebDriver(new URL(remote), opts);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid SELENIUM_URL: " + remote, e);
            }
        } else {
            driver = new ChromeDriver(opts);
        }
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
        // 옵션 원문 텍스트로 선택(문구가 약간 바뀌어도 동작)
        WebElement opt = selectEl.findElement(By.xpath(".//option[contains(normalize-space(.),'" + containsKo + "')]"));
        new Select(selectEl).selectByVisibleText(opt.getText());

        // 테이블 리로드 대기
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("tbl-type06")));
    }

    /** WebElement row를 파싱하여 Map으로 반환 */
    private Map<String, Object> parseRowData(WebElement row, LocalDate currentDate, String homeTeam, String awayTeam) {
        Map<String, Object> data = new HashMap<>();
        
        // 스코어 파싱
        Integer awayScore = null, homeScore = null;
        KboGame.Status status = KboGame.Status.SCHEDULED;
        WebElement play = row.findElement(By.cssSelector("td.play"));
        List<WebElement> ems = play.findElements(By.tagName("em"));
        if (!ems.isEmpty()) {
            String scoreTxt = ems.get(0).getText().trim();
            Matcher sc = Pattern.compile("(\\d+)\\s*vs\\s*(\\d+)").matcher(scoreTxt);
            if (sc.find()) {
                awayScore = parseIntSafe(sc.group(1));
                homeScore = parseIntSafe(sc.group(2));
                status = KboGame.Status.PLAYED;
            }
        }
        
        // 구장/비고
        WebElement relayCell = row.findElement(By.cssSelector("td.relay"));
        String stadium = TEAM_STADIUM.getOrDefault(
                homeTeam,
                relayCell.findElement(By.xpath("following-sibling::td[4]")).getText().trim()
        );
        String rawNote = relayCell.findElement(By.xpath("following-sibling::td[5]")).getText().trim();
        String note = (rawNote.isEmpty() || "-".equals(rawNote)) ? null : rawNote;
        
        if (note != null) {
            status = KboGame.Status.CANCELED;
            awayScore = homeScore = null;
        }
        
        // winTeam 계산
        String winTeam = null;
        if (awayScore != null && homeScore != null) {
            if (homeScore > awayScore) {
                winTeam = homeTeam;
            } else if (homeScore < awayScore) {
                winTeam = awayTeam;
            } else {
                winTeam = "무승부";
            }
        }
        
        // Map에 저장
        data.put("status", status.name());
        data.put("homeScore", homeScore);
        data.put("awayScore", awayScore);
        data.put("stadium", stadium);
        data.put("note", note);
        data.put("winTeam", winTeam);
        
        return data;
    }

    /** 크롤링 단계: Selenium으로 크롤링하여 game_raw에 저장 */
    private void crawlGames(int year, int month) {
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new","--no-sandbox","--disable-dev-shm-usage","--disable-gpu","--window-size=1920,1080");

        WebDriver driver;
        String remote = System.getenv("SELENIUM_URL");
        if (remote != null && !remote.isBlank()) {
            try {
                driver = new RemoteWebDriver(new URL(remote), opts);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid SELENIUM_URL: " + remote, e);
            }
        } else {
            driver = new ChromeDriver(opts);
        }
        
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
            crawlTableAndSave(driver, wait, year);

            // 2) 포스트시즌
            selectCategoryLoosely(wait, "포스트시즌");
            crawlTableAndSave(driver, wait, year);

        } finally {
            driver.quit();
        }
    }

    /** 테이블 크롤링 및 파싱하여 game_raw에 저장 (크롤링 + 파싱 함께 수행) */
    private void crawlTableAndSave(WebDriver driver, WebDriverWait wait, int year) {
        WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("tbl-type06")));
        WebElement tbody = table.findElement(By.tagName("tbody"));
        List<WebElement> rows = tbody.findElements(By.tagName("tr"));

        LocalDate currentDate = null;

        for (WebElement row : rows) {
            if (row.getText().contains("데이터가 없습니다")) continue;

            // 날짜 파싱
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

            // 시간 파싱
            List<WebElement> timeB = row.findElements(By.cssSelector("td.time b"));
            if (timeB.isEmpty()) continue;
            LocalTime time = safeParseTime(timeB.get(0).getText());
            if (time == null) continue;

            // 팀 파싱
            WebElement play = row.findElement(By.cssSelector("td.play"));
            List<WebElement> spans = play.findElements(By.tagName("span"));
            if (spans.size() < 2) continue;
            String awayTeam = spans.get(0).getText().trim();
            String homeTeam = spans.get(spans.size() - 1).getText().trim();

            try {
                // 파싱 로직 실행 (기존 로직 재사용)
                Map<String, Object> parsedData = parseRowData(row, currentDate, homeTeam, awayTeam);
                
                // JSON으로 변환
                String parsedJson = objectMapper.writeValueAsString(parsedData);
                
                // 중복 체크
                Optional<GameRaw> existing = gameRawRepo.findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
                        currentDate, time, homeTeam, awayTeam
                );

                String rowHtml = row.getAttribute("outerHTML");

                if (existing.isPresent()) {
                    GameRaw raw = existing.get();
                    raw.markCrawled(rowHtml);
                    raw.markParsed(parsedJson);
                    gameRawRepo.save(raw);
                } else {
                    GameRaw raw = new GameRaw(
                            currentDate, time, homeTeam, awayTeam,
                            rowHtml, parsedJson, GameRaw.Status.PARSED, 0, null
                    );
                    gameRawRepo.save(raw);
                }
            } catch (Exception e) {
                // 파싱 실패 시 FAILED 상태로 저장
                Optional<GameRaw> existing = gameRawRepo.findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
                        currentDate, time, homeTeam, awayTeam
                );
                
                if (existing.isPresent()) {
                    GameRaw raw = existing.get();
                    raw.markFailed("파싱 실패: " + e.getMessage());
                    gameRawRepo.save(raw);
                } else {
                    GameRaw raw = new GameRaw(
                            currentDate, time, homeTeam, awayTeam,
                            row.getAttribute("outerHTML"), null, GameRaw.Status.FAILED, 0,
                            "파싱 실패: " + e.getMessage()
                    );
                    gameRawRepo.save(raw);
                }
            }
        }
    }

    /** 파싱 단계: 실패한 건 재시도 (선택적) */
    private void parseGames() {
        // 크롤링 시점에 이미 파싱까지 완료하므로
        // 이 메서드는 실패한 건 재시도용으로만 사용
        // 현재는 간단히 스킵 (나중에 필요하면 구현)
        
        // TODO: FAILED 상태인 건 재시도 로직 (선택적)
    }

    /** 확정 단계: game_raw의 PARSED 상태를 games 테이블에 저장 */
    private void confirmGames() {
        List<GameRaw> parsedGames = gameRawRepo.findByStatus(GameRaw.Status.PARSED);
        
        for (GameRaw raw : parsedGames) {
            try {
                // JSON 파싱
                Map<String, Object> data = parseJsonToMap(raw.getParsedData());
                
                // 기존 KboGame 조회/생성
                Optional<KboGame> existing = gameRepo.findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
                        raw.getGameDate(), raw.getGameTime(), raw.getHomeTeam(), raw.getAwayTeam()
                );
                
                KboGame game = existing.orElseGet(() ->
                        KboGame.of(
                                raw.getGameDate(),
                                raw.getGameTime(),
                                raw.getHomeTeam(),
                                raw.getAwayTeam()
                        )
                );
                
                // applyScrape 호출
                KboGame.Status status = parseStatus((String) data.get("status"));
                Integer homeScore = data.get("homeScore") != null ? (Integer) data.get("homeScore") : null;
                Integer awayScore = data.get("awayScore") != null ? (Integer) data.get("awayScore") : null;
                String stadium = (String) data.get("stadium");
                String note = (String) data.get("note");
                String winTeam = (String) data.get("winTeam");
                
                game.applyScrape(status, homeScore, awayScore, stadium, note, winTeam);
                gameRepo.save(game);
                
            } catch (Exception e) {
                System.err.println("[KBO Crawler] Confirm failed for game: " + raw.getId() + " - " + e.getMessage());
            }
        }
    }

    /** JSON 문자열을 Map으로 변환 */
    private Map<String, Object> parseJsonToMap(String json) throws Exception {
        return objectMapper.readValue(json, Map.class);
    }

    /** 문자열을 KboGame.Status로 변환 */
    private KboGame.Status parseStatus(String statusStr) {
        if (statusStr == null) return KboGame.Status.SCHEDULED;
        try {
            return KboGame.Status.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return KboGame.Status.SCHEDULED;
        }
    }

    /** 표 파싱 & upsert: 실제 경기 행만 저장, 저장 건수 반환 */
    @Deprecated
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

            // 구장/비고
            WebElement relayCell = row.findElement(By.cssSelector("td.relay"));
            String stadium = TEAM_STADIUM.getOrDefault(
                    homeTeam,
                    relayCell.findElement(By.xpath("following-sibling::td[4]")).getText().trim()
            );
            String rawNote   = relayCell.findElement(By.xpath("following-sibling::td[5]")).getText().trim();
            String note      = (rawNote.isEmpty() || "-".equals(rawNote)) ? null : rawNote;

            if (note != null) {
                status = Status.CANCELED;
                awayScore = homeScore = null;
            }

            // upsert
            Optional<KboGame> opt = gameRepo.findByGameDateAndGameTimeAndHomeTeamAndAwayTeam(
                    currentDate, time, homeTeam, awayTeam
            );
            KboGame game = opt.isPresent()
                    ? opt.get()
                    : KboGame.of(currentDate, time, homeTeam, awayTeam);

            // apply scraped data
            game.applyScrape(
                    status,
                    homeScore,
                    awayScore,
                    stadium,
                    note,
                    (awayScore != null && homeScore != null
                            ? (homeScore > awayScore ? homeTeam
                            : homeScore < awayScore ? awayTeam
                            : "무승부")
                            : null)
            );

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
        s = s.replaceAll("[^0-9:]", "");
        try {
            return LocalTime.parse(s)
                    .truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntSafe(String n) {
        try {
            return Integer.valueOf(n.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeTv(String raw, int maxLen) {
        if (raw == null) return null;
        String s = raw.replace("\r","").replace("\n", ", ").trim();
        if (s.isEmpty() || "-".equals(s)) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    // ―― 홈팀 → 홈구장 매핑 ――
    private static final Map<String, String> TEAM_STADIUM = Map.of(
            "두산", "서울잠실야구장",
            "LG",   "서울잠실야구장",
            "키움", "고척스카이돔",
            "삼성", "대구삼성라이온즈파크",
            "롯데", "사직야구장",
            "KIA",  "광주기아챔피언스필드",
            "한화", "대전한화생명볼파크",
            "SSG",  "인천SSG랜더스필드",
            "NC",   "창원NC파크",
            "KT",   "수원케이티위즈파크"
    );
}