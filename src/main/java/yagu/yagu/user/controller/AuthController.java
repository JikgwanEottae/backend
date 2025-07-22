package yagu.yagu.user.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository repo;

    /** 닉네임 중복 체크 */
    @GetMapping("/nickname/check")
    public ResponseEntity<Map<String,Boolean>> checkNickname(@RequestParam String nickname) {
        boolean ok = !repo.existsByNickname(nickname);
        return ResponseEntity.ok(Map.of("available", ok));
    }

    /** 프로필 완성 */
    @PostMapping("/profile")
    public ResponseEntity<Map<String,Boolean>> completeProfile(
            @RequestBody ProfileReq req,
            Authentication auth) {

        String email = auth.getName();
        User u = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
        u.completeProfile(req.getNickname());
        repo.save(u);
        return ResponseEntity.ok(Map.of("profileCompleted", true));
    }

    @Data
    static class ProfileReq { private String nickname; }
}
