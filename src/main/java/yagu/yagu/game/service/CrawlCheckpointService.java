package yagu.yagu.game.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import yagu.yagu.game.entity.CrawlCheckpoint;
import yagu.yagu.game.repository.CrawlCheckpointRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CrawlCheckpointService {
    private final CrawlCheckpointRepository checkpointRepository;

    public LocalDate getLastSuccessDate(String jobName) {
        return checkpointRepository.findByJobName(jobName)
                .map(CrawlCheckpoint::getLastSuccessDate)
                .orElse(LocalDate.now().minusDays(7));
    }

    public void updateLastSuccessDate(String jobName, LocalDate date) {
        CrawlCheckpoint checkpoint = checkpointRepository.findByJobName(jobName)
                .orElse(CrawlCheckpoint.of(jobName));
        checkpoint.updateDate(date);
        checkpointRepository.save(checkpoint);
    }
}
