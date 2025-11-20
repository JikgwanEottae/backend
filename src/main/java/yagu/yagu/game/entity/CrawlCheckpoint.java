package yagu.yagu.game.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_checkpoint")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlCheckpoint {
    @Id
    @Column(name = "job_name", length = 50)
    private String jobName;

    @Column(name = "last_success_date")
    private LocalDate lastSuccessDate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CrawlCheckpoint(String jobName, LocalDate lastSuccessDate) {
        this.jobName = jobName;
        this.lastSuccessDate = lastSuccessDate;
    }

    public static CrawlCheckpoint of(String jobName) {
        return new CrawlCheckpoint(jobName, null);
    }

    @PrePersist
    @PreUpdate
    public void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDate(LocalDate date) {
        this.lastSuccessDate = date;
    }
}
