package yagu.yagu.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.game.entity.CrawlCheckpoint;

import java.util.Optional;

public interface CrawlCheckpointRepository extends JpaRepository<CrawlCheckpoint, String> {
    Optional<CrawlCheckpoint> findByJobName(String jobName);
}
