package yagu.yagu.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "game_raw")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameRaw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "game_time")
    private LocalTime gameTime;

    @Column(name = "home_team", length = 50)
    private String homeTeam;

    @Column(name = "away_team", length = 50)
    private String awayTeam;

    @Column(name = "raw_html", columnDefinition = "TEXT")
    private String rawHtml;

    @Column(name = "parsed_data", columnDefinition = "TEXT")
    private String parsedData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        CRAWLED, // 크롤링 완료(파싱 전)
        PARSED, // 파싱 완료(확정 대기)
        FAILED // 실패(재시도 대상)
    }

    public GameRaw(LocalDate gameDate, LocalTime gameTime, String homeTeam, String awayTeam,
            String rawHtml, String parsedData, Status status, Integer retryCount, String errorMessage) {
        this.gameDate = gameDate;
        this.gameTime = gameTime;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.rawHtml = rawHtml;
        this.parsedData = parsedData;
        this.status = status != null ? status : Status.CRAWLED;
        this.retryCount = retryCount != null ? retryCount : 0;
        this.errorMessage = errorMessage;
    }

    public static GameRaw of(LocalDate gameDate, LocalTime gameTime, String homeTeam, String awayTeam,
            String rawHtml, Status status) {
        return new GameRaw(gameDate, gameTime, homeTeam, awayTeam, rawHtml, null, status, 0, null);
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        if (this.status == null) {
            this.status = Status.CRAWLED;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 상태 변경 헬퍼 메서드
    public void markParsed(String parsedData) {
        this.parsedData = parsedData;
        this.status = Status.PARSED;
        this.retryCount = 0;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = Status.FAILED;
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.errorMessage = errorMessage;
    }

    public void markCrawled(String rawHtml) {
        this.rawHtml = rawHtml;
        this.status = Status.CRAWLED;
        this.retryCount = 0;
        this.errorMessage = null;
    }
}
