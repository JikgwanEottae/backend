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


        if (year == curYear) {
            String json = redis.opsForValue().get(key);
            if (json != null) return fromJson(json);
            List<KboGameDTO> list = loadFromDb(ym);
            putMonth(ym, list); // 무기한 저장 + 인덱스 등록(올해만)
            return list;
        } else {
            String json = redis.opsForValue().get(key);
            return (json != null) ? fromJson(json) : loadFromDb(ym);
        }
    }


    /** 매일 00:25에 현재 달 강제 최신화 */
    public void refreshCurrentMonth() {
        YearMonth now = YearMonth.now(KST);
        putMonth(now, loadFromDb(now));
    }


    /** 앱 기동 시 1회 워밍(1월~현재달) */
    public void warmCurrentYearUpToNow() {
        YearMonth now = YearMonth.now(KST);
        YearMonth cur = YearMonth.of(now.getYear(), 1);
        for (; !cur.isAfter(now); cur = cur.plusMonths(1)) {
            putMonth(cur, loadFromDb(cur));
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
        if (ym.getYear() != curYear) return;
        String key = CalendarCacheKeys.monthKey(ym);
        try {
            redis.opsForValue().set(key, om.writeValueAsString(list));
        } catch (Exception e) { throw new RuntimeException(e); }
        redis.opsForSet().add(CalendarCacheKeys.yearIndexKey(curYear), key);
        redis.opsForSet().add(CalendarCacheKeys.YEARS_SET, String.valueOf(curYear));
    }


    private List<KboGameDTO> fromJson(String json) {
        try { return om.readValue(json, new TypeReference<List<KboGameDTO>>(){}); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}