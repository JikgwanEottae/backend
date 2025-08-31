package yagu.yagu.game.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.game.cache.GameCalendarCacheService;
import yagu.yagu.game.dto.KboGameDTO;
import yagu.yagu.game.entity.KboGame;
import yagu.yagu.game.repository.KboGameRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final KboGameRepository gameRepo;
    private final GameCalendarCacheService calendarCache;

    public GameController(KboGameRepository gameRepo, GameCalendarCacheService calendarCache) {
        this.gameRepo = gameRepo;
        this.calendarCache = calendarCache;
    }

    /** 날짜별 경기 조회 */
    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<KboGameDTO>>> getGamesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<KboGameDTO> dtos = gameRepo.findByGameDate(date)
                .stream()
                .map(KboGameDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(dtos, "날짜별 경기 조회 완료"));
    }

    /** 단일 경기 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KboGameDTO>> getGameById(@PathVariable Long id) {
        KboGame game = gameRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        return ResponseEntity.ok(ApiResponse.success(new KboGameDTO(game), "경기 상세 조회 완료"));
    }

    /** 월별 캘린더용 조회 (쿼리 파라미터) */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<KboGameDTO>>> getMonthlyGames(@RequestParam int year, @RequestParam int month) {
        if (year < 1900 || year > 2100) throw new BusinessException(ErrorCode.GAME_DATE_INVALID, "연도는 1900~2100 사이여야 합니다.");
        if (month < 1 || month > 12) throw new BusinessException(ErrorCode.GAME_DATE_INVALID, "월은 1~12 사이여야 합니다.");


        List<KboGameDTO> dtos = calendarCache.getMonthly(year, month);
        return ResponseEntity.ok(ApiResponse.success(dtos, "월별 경기 조회 완료"));
    }
}