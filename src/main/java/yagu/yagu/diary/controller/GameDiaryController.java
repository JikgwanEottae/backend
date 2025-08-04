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
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.entity.UserStats;
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
        public ResponseEntity<ApiResponse<Map<String, Long>>> create(
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
                        return ResponseEntity.created(location)
                                .body(ApiResponse.created(Map.of("diaryId", id), "일기 작성 완료"));
                } catch (RuntimeException e) {
                        if (e.getMessage() != null && e.getMessage().contains("User not found")) {
                                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
                        }
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, e.getMessage());
                }
        }

        // 일기 수정 (멀티파트: dto + file[optional])
        @PostMapping(value = "/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ApiResponse<Void>> update(
                @AuthenticationPrincipal CustomOAuth2User principal,
                @PathVariable Long diaryId,
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
                        // dto.photoUrl == null 이면 서비스에서 기존 URL 유지
                        service.updateDiary(userId, diaryId, dto);
                        return ResponseEntity.ok(ApiResponse.success(null, "일기 수정 완료"));
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
