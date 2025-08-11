package yagu.yagu.diary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.common.security.CustomOAuth2User;
import yagu.yagu.diary.dto.CreateGameDiaryDTO;
import yagu.yagu.diary.dto.UpdateGameDiaryDTO;
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.dto.UserStatsDTO;
import yagu.yagu.diary.service.GameDiaryService;
import yagu.yagu.image.service.ImageService;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
public class GameDiaryController {
        private final GameDiaryService service;
        private final ImageService imageService;

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
                        if (e.getMessage() != null && e.getMessage().contains("User not found")) {
                                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                        }
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
                        if (e.getMessage() != null && e.getMessage().contains("Diary not found")) {
                                throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 신규 작성 (멀티파트: dto + file[optional])
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<List<Map<String, Long>>>> create(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @RequestPart("dto") CreateGameDiaryDTO dto,
                        @RequestPart(value = "file", required = false) MultipartFile file) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }
                try {
                        Long userId = principal.getUser().getId();

                        if (file != null && !file.isEmpty()) {
                                String url = imageService.upload(file);
                                dto.setPhotoUrl(url);
                        }
                        Long id = service.createDiary(userId, dto);
                        URI location = URI.create("/api/diaries/" + id);

                        List<Map<String, Long>> result = List.of(Map.of("diaryId", id)); // ← 배열 형태로 래핑
                        return ResponseEntity.created(location)
                                        .body(ApiResponse.created(result, "일기 작성 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage() != null && e.getMessage().contains("User not found")) {
                                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 일기 수정 (멀티파트: dto + file[optional])
        @PostMapping(value = "/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<List<Map<String, Long>>>> update(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @PathVariable Long diaryId,
                        @RequestPart("dto") UpdateGameDiaryDTO dto,
                        @RequestPart(value = "file", required = false) MultipartFile file) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }
                try {
                        Long userId = principal.getUser().getId();

                        if (file != null && !file.isEmpty()) {
                                String url = imageService.upload(file);
                                dto.setPhotoUrl(url);
                        } else {
                                // 파일 파트가 없을 때는 프론트의 의도를 따름
                                // - dto.photoUrl == null -> 삭제 의도로 간주해 빈 문자열 세팅
                                // - dto.photoUrl != null -> 프론트가 유지/교체 의도를 명시했으므로 그대로 둠
                                if (dto.getPhotoUrl() == null) {
                                        dto.setPhotoUrl("");
                                }
                        }
                        // 서비스 호출 (void 반환)
                        service.updateDiary(userId, diaryId, dto);

                        List<Map<String, Long>> result = List.of(Map.of("diaryId", diaryId));
                        return ResponseEntity.ok(
                                        ApiResponse.success(result, "일기 수정 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage() != null && e.getMessage().contains("Diary not found")) {
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
                        if (e.getMessage() != null && e.getMessage().contains("Diary not found")) {
                                throw new BusinessException(ErrorCode.DIARY_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 월별 조회 API
        @GetMapping("/month")
        public ResponseEntity<ApiResponse<List<GameDiaryDetailDTO>>> byMonth(
                        @AuthenticationPrincipal CustomOAuth2User principal,
                        @RequestParam int year,
                        @RequestParam int month) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }

                // year, month validation (optional)
                if (month < 1 || month > 12) {
                        throw new BusinessException(ErrorCode.INVALID_MONTH);
                }

                Long userId = principal.getUser().getId();
                List<GameDiaryDetailDTO> diaries = service.getDiariesByMonth(userId, year, month);
                return ResponseEntity.ok(
                                ApiResponse.success(diaries, year + "년 " + month + "월 일기 조회 완료"));
        }

        // 승률 조회
        @GetMapping("/stats")
        public ResponseEntity<ApiResponse<UserStatsDTO>> getStats(
                        @AuthenticationPrincipal CustomOAuth2User principal) {

                if (principal == null || principal.getUser() == null) {
                        throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
                }
                try {
                        Long userId = principal.getUser().getId();
                        UserStatsDTO stats = service.getUserStats(userId);
                        return ResponseEntity.ok(ApiResponse.success(stats, "승률 조회 완료"));
                } catch (RuntimeException e) {
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }
}
