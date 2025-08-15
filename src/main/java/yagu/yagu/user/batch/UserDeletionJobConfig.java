package yagu.yagu.user.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;
import yagu.yagu.user.service.AuthService;

import java.time.Instant;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class UserDeletionJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Bean
    public Job purgeDeletedUsersJob() {
        return new JobBuilder("purgeDeletedUsersJob", jobRepository)
                .start(purgeDeletedUsersStep())
                .build();
    }

    @Bean
    public Step purgeDeletedUsersStep() {
        return new StepBuilder("purgeDeletedUsersStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Instant now = Instant.now();
                    List<User> targets = userRepository.findByDeletedAtIsNotNullAndPurgeAtBefore(now);
                    for (User user : targets) {
                        authService.hardDeleteUser(user);
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
