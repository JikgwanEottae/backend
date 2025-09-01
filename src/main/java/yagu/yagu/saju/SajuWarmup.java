package yagu.yagu.saju;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import yagu.yagu.saju.config.FastApiProperties;

@Component
public class SajuWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SajuWarmup.class);

    private final RestTemplate restTemplate;
    private final FastApiProperties props;

    public SajuWarmup(RestTemplate restTemplate, FastApiProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        final String url = props.getBaseUrl() + "/health";
        final int maxAttempts = 3;
        final long backoffMs = 400L;

        for (int i = 1; i <= maxAttempts; i++) {
            try {
                ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
                log.info("[SAJU-WARMUP] FastAPI {} -> status={} body={}",
                        url, resp.getStatusCode(), resp.getBody());
                return;
            } catch (Exception e) {
                log.warn("[SAJU-WARMUP] attempt {}/{} failed: {}", i, maxAttempts, e.toString());
                try { Thread.sleep(backoffMs); } catch (InterruptedException ignored) {}
            }
        }

        log.warn("[SAJU-WARMUP] FastAPI health warmup skipped after {} attempts.", maxAttempts);
    }
}