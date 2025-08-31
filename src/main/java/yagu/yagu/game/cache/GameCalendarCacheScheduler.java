package yagu.yagu.game.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static yagu.yagu.game.cache.CalendarCacheKeys.YEARS_SET;
import static yagu.yagu.game.cache.CalendarCacheKeys.yearIndexKey;

@Component
@RequiredArgsConstructor
public class GameCalendarCacheScheduler {
    private final GameCalendarCacheService cache;
    private final StringRedisTemplate redis;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");


    @Scheduled(cron = "0 25 0 * * *", zone = "Asia/Seoul")
    public void refreshAndKeepOnlyCurrentYear() {
        cache.refreshCurrentMonth();


        int curYear = Year.now(KST).getValue();
        Set<String> years = redis.opsForSet().members(YEARS_SET);
        if (years == null || years.isEmpty()) return;


        List<String> removeYears = new ArrayList<>();
        for (String y : years) {
            if (!String.valueOf(curYear).equals(y)) {
                String indexKey = yearIndexKey(Integer.parseInt(y));
                Set<String> keys = redis.opsForSet().members(indexKey);
                redis.executePipelined((RedisCallback<Object>) conn -> {
                    if (keys != null) {
                        for (String k : keys) conn.keyCommands().del(k.getBytes(StandardCharsets.UTF_8));
                    }
                    conn.keyCommands().del(indexKey.getBytes(StandardCharsets.UTF_8));
                    return null;
                });
                removeYears.add(y);
            }
        }
        if (!removeYears.isEmpty()) {
            redis.opsForSet().remove(YEARS_SET, removeYears.toArray());
            redis.opsForSet().add(YEARS_SET, String.valueOf(curYear));
        }
    }
}
