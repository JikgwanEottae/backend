package yagu.yagu.game.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yagu.yagu.game.dto.KboGameDTO;
import yagu.yagu.game.repository.KboGameRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final KboGameRepository gameRepo;

    public GameController(KboGameRepository gameRepo) {
        this.gameRepo = gameRepo;
    }

    /** 전체 경기 조회 */
    @GetMapping
    public ResponseEntity<List<KboGameDTO>> getAllGames() {
        List<KboGameDTO> dtos = gameRepo.findAll()
                .stream()
                .map(KboGameDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /** 날짜별 경기 조회 */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<KboGameDTO>> getGamesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<KboGameDTO> dtos = gameRepo.findByGameDate(date)
                .stream()
                .map(KboGameDTO::new)
                .collect(Collectors.toList());
        if (dtos.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dtos);
    }

    /** 단일 경기 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<KboGameDTO> getGameById(@PathVariable Long id) {
        return gameRepo.findById(id)
                .map(KboGameDTO::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 월별 캘린더용 데이터 조회 */
    @GetMapping("/calendar/{year}/{month}")
    public ResponseEntity<List<KboGameDTO>> getMonthlyGames(
            @PathVariable int year,
            @PathVariable int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<KboGameDTO> dtos = gameRepo.findByGameDateBetween(start, end).stream()
                .map(KboGameDTO::new)
                .collect(Collectors.toList());
        if (dtos.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dtos);
    }
}