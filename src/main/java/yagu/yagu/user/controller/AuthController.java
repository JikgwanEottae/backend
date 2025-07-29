package yagu.yagu.user.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.common.security.CustomOAuth2User;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;
import yagu.yagu.user.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepo;
    private final AuthService authService;

    /** 로그인 상태 체크 & 유저 정보 반환 */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();
        Map<String, Object> data = authService.createLoginResponse(user);
        return ResponseEntity.ok(ApiResponse.success(data, "로그인 상태 확인 완료"));
    }

    /** 닉네임 중복 체크 */
    @GetMapping("/nickname/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkNickname(@RequestParam String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "닉네임을 입력해주세요.");
        }

        boolean available = !userRepo.existsByNickname(nickname);
        return ResponseEntity.ok(ApiResponse.success(Map.of("available", available), "닉네임 중복 확인 완료"));
    }

    /** 프로필 완성 — profileCompleted = true 로 업뎃 */
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> completeProfile(
            @RequestBody ProfileReq req,
            Authentication authentication) {

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (!StringUtils.hasText(req.getNickname())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "닉네임을 입력해주세요.");
        }

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        // 닉네임 중복 체크 (본인 제외)
        if (userRepo.existsByNickname(req.getNickname()) &&
                !req.getNickname().equals(user.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        user.completeProfile(req.getNickname());
        userRepo.save(user);

        return ResponseEntity.ok(ApiResponse.success(Map.of("profileCompleted", true), "프로필 설정 완료"));
    }

    @Data
    static class ProfileReq {
        private String nickname;
    }
}
