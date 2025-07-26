package yagu.yagu.diary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.security.CustomOAuth2User;
import yagu.yagu.diary.dto.CreateGameDiaryDTO;
import yagu.yagu.diary.dto.GameDiaryCalendarDTO;
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.service.GameDiaryService;

import java.util.List;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class GameDiaryController {
    private final GameDiaryService service;

    // 1) 월별 캘린더용 메타데이터
    @GetMapping("/calendar")
    public ResponseEntity<List<GameDiaryCalendarDTO>> monthly(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(service.getMonthlyDiaries(userId, year, month));
    }

    // 2) 상세 조회
    @GetMapping("/{diaryId}")
    public ResponseEntity<GameDiaryDetailDTO> detail(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long diaryId) {
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(service.getDiaryDetail(userId, diaryId));
    }

    // 3) 신규 작성
    @PostMapping
    public ResponseEntity<Long> create(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody CreateGameDiaryDTO dto) {
        Long userId = principal.getUser().getId();
        Long id = service.createDiary(userId, dto);
        return ResponseEntity.ok(id);
    }
}
