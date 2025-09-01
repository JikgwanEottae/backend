package yagu.yagu.game.cache;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import yagu.yagu.game.dto.KboGameDTO;
import yagu.yagu.game.repository.KboGameRepository;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameCalendarCacheService {
    private final StringRedisTemplate redis;
    private final ObjectMapper om;
    private final KboGameRepository repo;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");


    /** 컨트롤러에서 호출: 월별 경기 (현재 연도면 캐시 사용/쓰기) */
    public List<KboGameDTO> getMonthly(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        int curYear = Year.now(KST).getValue();
        String key = CalendarCacheKeys.monthKey(ym);

        // 캐시에 있으면 바로 반환
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            return fromJson(cached);
        }

        // 캐시에 없으면 DB 조회
        List<KboGameDTO> list = loadFromDb(ym);

        // 현재 연도에 한해: 빈 결과는 캐시하지 않기
        if (year == curYear && !list.isEmpty()) {
            putMonth(ym, list);
        }

        return list;
    }


    /** 매일 00:25에 현재 달 강제 최신화 */
    public void refreshCurrentMonth() {
        YearMonth now = YearMonth.now(KST);
        List<KboGameDTO> list = loadFromDb(now);

        String key = CalendarCacheKeys.monthKey(now);
        if (list.isEmpty()) {
            // 빈 결과면 기존 키가 있더라도 삭제해 "빈-캐시"가 남지 않게 함
            redis.delete(key);
            // 인덱스에서 키가 있다면 제거(옵션)
            removeFromYearIndex(now.getYear(), key);
            return;
        }
        putMonth(now, list);
    }


    /** 앱 기동 시 1회 워밍(1월~현재달) */
    public void warmCurrentYearUpToNow() {
        YearMonth now = YearMonth.now(KST);
        YearMonth cur = YearMonth.of(now.getYear(), 1);
        for (; !cur.isAfter(now); cur = cur.plusMonths(1)) {
            List<KboGameDTO> list = loadFromDb(cur);
            if (!list.isEmpty()) {
                putMonth(cur, list);
            } else {
                // 혹시 초기 워밍 전에 빈 결과가 들어갔었다면 정리(보수적)
                String key = CalendarCacheKeys.monthKey(cur);
                redis.delete(key);
                removeFromYearIndex(cur.getYear(), key);
            }
        }
    }


    // --- 내부 유틸 ---
    private List<KboGameDTO> loadFromDb(YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return repo.findByGameDateBetween(start, end).stream().map(KboGameDTO::new).collect(Collectors.toList());
    }


    private void putMonth(YearMonth ym, List<KboGameDTO> list) {
        int curYear = Year.now(KST).getValue();
        if (ym.getYear() != curYear) return; // 현재 연도만 캐시

        String key = CalendarCacheKeys.monthKey(ym);
        try {
            redis.opsForValue().set(key, om.writeValueAsString(list));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        redis.opsForSet().add(CalendarCacheKeys.yearIndexKey(curYear), key);
        redis.opsForSet().add(CalendarCacheKeys.YEARS_SET, String.valueOf(curYear));
    }

    private void removeFromYearIndex(int year, String key) {
        try {
            redis.opsForSet().remove(CalendarCacheKeys.yearIndexKey(year), key);
            Set<String> members = redis.opsForSet().members(CalendarCacheKeys.yearIndexKey(year));
            if (members == null || members.isEmpty()) {
                redis.opsForSet().remove(CalendarCacheKeys.YEARS_SET, String.valueOf(year));
            }
        } catch (Exception ignore) {
        }
    }

    private List<KboGameDTO> fromJson(String json) {
        try { return om.readValue(json, new TypeReference<List<KboGameDTO>>(){}); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}