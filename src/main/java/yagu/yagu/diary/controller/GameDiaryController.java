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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class GameDiaryController {
    private final GameDiaryService service;

    // 전체 조회
    @GetMapping
    public ResponseEntity<List<GameDiaryDetailDTO>> all(
            @AuthenticationPrincipal CustomOAuth2User principal) {
        Long userId = principal.getUser().getId();
        return ResponseEntity
                .ok()
                .body(service.getAllDiaries(userId));
    }

    // 월별 캘린더용 메타데이터
    @GetMapping("/calendar")
    public ResponseEntity<List<GameDiaryCalendarDTO>> monthly(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = principal.getUser().getId();
        return ResponseEntity
                .ok()
                .body(service.getMonthlyDiaries(userId, year, month));
    }

    //  상세 조회
    @GetMapping("/{diaryId}")
    public ResponseEntity<GameDiaryDetailDTO> detail(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long diaryId) {
        Long userId = principal.getUser().getId();
        return ResponseEntity
                .ok()
                .body(service.getDiaryDetail(userId, diaryId));
    }

    // 신규 작성
    @PostMapping
    public ResponseEntity<Map<String, Long>> create(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestBody CreateGameDiaryDTO dto) {
        Long userId = principal.getUser().getId();
        Long id = service.createDiary(userId, dto);


        URI location = URI.create("/api/diaries/" + id);

        return ResponseEntity
                .created(location)
                .body(Collections.singletonMap(
                        "diaryId", id
                ));
    }
}
