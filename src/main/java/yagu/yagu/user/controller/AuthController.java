package yagu.yagu.user.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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

    /**  로그인 상태 체크 & 유저 정보 반환 */
    @GetMapping("/check")
    public ResponseEntity<Map<String,Object>> checkAuth(Authentication authentication) {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();
        Map<String,Object> data = authService.createLoginResponse(user);
        return ResponseEntity.ok(data);
    }

    /**  닉네임 중복 체크 */
    @GetMapping("/nickname/check")
    public ResponseEntity<Map<String,Boolean>> checkNickname(@RequestParam String nickname) {
        boolean available = !userRepo.existsByNickname(nickname);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**  프로필 완성 — profileCompleted = true 로 업뎃 */
    @PostMapping("/profile")
    public ResponseEntity<Map<String,Boolean>> completeProfile(
            @RequestBody ProfileReq req,
            Authentication authentication) {

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();
        user.completeProfile(req.getNickname());
        userRepo.save(user);

        return ResponseEntity.ok(Map.of("profileCompleted", true));
    }

    @Data static class ProfileReq {
        private String nickname;
    }
}
