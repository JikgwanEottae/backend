package yagu.yagu.game.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GameCalendarWarmupRunner {
    private final GameCalendarCacheService cache;


    @Bean
    CommandLineRunner warmOnStart(@Value("${cache.calendar.warm-on-start:true}") boolean enabled) {
        return args -> { if (enabled) cache.warmCurrentYearUpToNow(); };
    }
}