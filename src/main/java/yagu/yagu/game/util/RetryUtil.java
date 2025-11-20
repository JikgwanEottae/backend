package yagu.yagu.game.util;

import java.util.Random;
import java.util.function.Supplier;

public class RetryUtil {
    private static final Random RANDOM = new Random();

    @SafeVarargs
    public static <T> T retryWithBackoff(
            Supplier<T> task,
            int maxRetries,
            Class<? extends Exception>... retryableExceptions) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return task.get();
            } catch (Exception e) {
                lastException = e;

                // 재시도 가능한 예외인지 체크
                if (retryableExceptions != null && retryableExceptions.length > 0) {
                    boolean retryable = false;
                    for (Class<? extends Exception> exType : retryableExceptions) {
                        if (exType.isInstance(e)) {
                            retryable = true;
                            break;
                        }
                    }
                    if (!retryable)
                        throw e;
                }

                if (attempt == maxRetries)
                    break;

                long baseDelay = (long) Math.pow(2, attempt - 1) * 1000L; // 1s, 2s, 4s, 8s...
                long jitter = (long) (baseDelay * 0.2 * (RANDOM.nextDouble() * 2 - 1)); // ±20%
                long delay = Math.max(500L, baseDelay + jitter); // 최소 0.5초

                Thread.sleep(delay);
            }
        }

        throw lastException != null ? lastException : new RuntimeException("Max retries exceeded");
    }
}
