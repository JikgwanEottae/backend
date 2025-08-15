package yagu.yagu.user.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {
    private final JobLauncher jobLauncher;
    private final Job purgeDeletedUsersJob;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * *")
    public void runDaily() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(purgeDeletedUsersJob, params);
    }
}
