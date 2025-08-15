package yagu.yagu.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yagu.yagu.common.response.ApiResponse;

@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class BatchController {
    private final JobLauncher jobLauncher;
    private final Job purgeDeletedUsersJob;

    @PostMapping("/purge-users")
    public ResponseEntity<ApiResponse<Void>> runPurgeUsers() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(purgeDeletedUsersJob, params);
        return ResponseEntity.ok(ApiResponse.success(null, "배치 실행 요청 완료"));
    }
}
