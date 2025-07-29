package yagu.yagu.diary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.common.security.CustomOAuth2User;
import yagu.yagu.diary.dto.CreateGameDiaryDTO;
import yagu.yagu.diary.dto.GameDiaryCalendarDTO;
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.entity.UserStats;
import yagu.yagu.diary.service.GameDiaryService;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class GameDiaryController {
        private final GameDiaryService service;

        // 전체 조회
        @GetMapping
        public ResponseEntity<ApiResponse<List<GameDiaryDetailDTO>>> all(
                        @AuthenticationPrincipal CustomOAuth2User principal) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                try {
                        Long userId = principal.getUser().getId();
                        List<GameDiaryDetailDTO> diaries = service.getAllDiaries(userId);
                        return ResponseEntity.ok(ApiResponse.success(diaries, "전체 일기 조회 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage().contains("User not found")) {
                                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 월별 캘린더용 메타데이터
        @GetMapping("/calendar")
        public ResponseEntity<ApiResponse<List<GameDiaryCalendarDTO>>> monthly(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @RequestParam int year,
                        @RequestParam int month) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                // 파라미터 유효성 검사
                if (year < 1900 || year > 2100) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST, "연도는 1900~2100 사이여야 합니다.");
                }
                if (month < 1 || month > 12) {
                        throw new BusinessException(ErrorCode.INVALID_REQUEST, "월은 1~12 사이여야 합니다.");
                }

                try {
                        Long userId = principal.getUser().getId();
                        List<GameDiaryCalendarDTO> diaries = service.getMonthlyDiaries(userId, year, month);
                        return ResponseEntity.ok(ApiResponse.success(diaries, "월별 일기 조회 완료"));
                } catch (RuntimeException e) {
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 상세 조회
        @GetMapping("/{diaryId}")
        public ResponseEntity<ApiResponse<GameDiaryDetailDTO>> detail(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @PathVariable Long diaryId) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                try {
                        Long userId = principal.getUser().getId();
                        GameDiaryDetailDTO diary = service.getDiaryDetail(userId, diaryId);
                        return ResponseEntity.ok(ApiResponse.success(diary, "일기 상세 조회 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage().contains("Diary not found")) {
                                throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 신규 작성
        @PostMapping
        public ResponseEntity<ApiResponse<Map<String, Long>>> create(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @RequestBody CreateGameDiaryDTO dto) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                try {
                        Long userId = principal.getUser().getId();
                        Long id = service.createDiary(userId, dto);

                        URI location = URI.create("/api/diaries/" + id);

                        return ResponseEntity.created(location)
                                        .body(ApiResponse.created(Map.of("diaryId", id), "일기 작성 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage().contains("User not found")) {
                                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 일기 수정
        @PostMapping("/{diaryId}")
        public ResponseEntity<ApiResponse<Void>> update(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @PathVariable Long diaryId,
                        @RequestBody CreateGameDiaryDTO dto) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                try {
                        Long userId = principal.getUser().getId();
                        service.updateDiary(userId, diaryId, dto);
                        return ResponseEntity.ok(ApiResponse.success(null, "일기 수정 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage().contains("Diary not found")) {
                                throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 일기 삭제
        @DeleteMapping("/{diaryId}")
        public ResponseEntity<ApiResponse<Void>> delete(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @PathVariable Long diaryId) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                try {
                        Long userId = principal.getUser().getId();
                        service.deleteDiary(userId, diaryId);
                        return ResponseEntity.ok(ApiResponse.success(null, "일기 삭제 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage().contains("Diary not found")) {
                                throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 승률 조회
        @GetMapping("/stats")
        public ResponseEntity<ApiResponse<UserStats>> getStats(
                        @AuthenticationPrincipal CustomOAuth2User principal) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                try {
                        Long userId = principal.getUser().getId();
                        UserStats stats = service.getUserStats(userId);
                        return ResponseEntity.ok(ApiResponse.success(stats, "승률 조회 완료"));
                } catch (RuntimeException e) {
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }
}
