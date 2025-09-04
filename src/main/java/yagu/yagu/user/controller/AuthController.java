package yagu.yagu.user.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.common.oauth.AppleJwtVerifier;
import yagu.yagu.common.oauth.KakaoApiClient;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.common.security.CustomOAuth2User;
import yagu.yagu.user.dto.LoginRequests;
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

    private final KakaoApiClient kakaoApiClient;
    private final AppleJwtVerifier appleJwtVerifier;


    @PostMapping("/login/kakao")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginKakao(
            @RequestBody LoginRequests.KakaoLoginRequest req,
            HttpServletResponse res
    ) {
        try {
            var ku = kakaoApiClient.me(req.accessToken());
            String preferredNick = (req.nickname() != null && !req.nickname().isBlank())
                    ? req.nickname() : ku.nickname();

            var user = authService.findOrCreateByProvider(
                    User.AuthProvider.KAKAO, ku.id(), ku.email(), preferredNick
            );

            // 서비스에서 Map<String,Object> (accessToken, refreshToken) 반환
            Map<String, Object> tokens = authService.createLoginResponse(user, res);
            return ResponseEntity.ok(ApiResponse.success(tokens, "로그인 성공"));
        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            int status = ("KAKAO_TOKEN_INVALID".equals(msg)) ? 401 : 502;

            return ResponseEntity.status(status).body(
                    ApiResponse.<Map<String, Object>>builder()
                            .result(false)
                            .httpCode(status)
                            .data(null)
                            .message(("KAKAO_TOKEN_INVALID".equals(msg))
                                    ? "카카오 토큰이 유효하지 않습니다."
                                    : "카카오 API 호출 중 오류가 발생했습니다.")
                            .build()
            );
        }
    }

    // Apple
    @PostMapping("/login/apple")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginApple(
            @RequestBody LoginRequests.AppleLoginRequest req,
            HttpServletResponse res
    ) {
        JWTClaimsSet claims = appleJwtVerifier.verify(req.identityToken());
        String sub   = claims.getSubject();
        String email = AppleJwtVerifier.extractEmailSafe(claims);
        String fallback = (email != null) ? email.split("@")[0] : "AppleUser";
        String preferredNick = (req.nickname() != null && !req.nickname().isBlank())
                ? req.nickname() : fallback;

        var user = authService.findOrCreateByProvider(
                User.AuthProvider.APPLE, sub, email, preferredNick
        );

        Map<String, Object> tokens = authService.createLoginResponse(user, res);
        return ResponseEntity.ok(ApiResponse.success(tokens, "로그인 성공"));
    }

    /** 로그인 상태 체크 & 유저 정보 반환 */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuth(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();
        Map<String, Object> data = Map.of(
                "email", user.getEmail(),
                "nickname", user.getNickname(),
                "provider", user.getProvider(),
                "profileCompleted", user.isProfileCompleted());
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

    /** 프로필 완성 — profileCompleted = true 로 업데이트 */
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
        if (userRepo.existsByNickname(req.getNickname())
                && !req.getNickname().equals(user.getNickname())) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
        user.completeProfile(req.getNickname());
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success(Map.of("profileCompleted", true), "프로필 설정 완료"));
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        var principal = (CustomOAuth2User) authentication.getPrincipal();
        authService.logout(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃 되었습니다"));
    }

    /** 회원탈퇴 */
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(Authentication authentication) {
        var principal = (CustomOAuth2User) authentication.getPrincipal();
        authService.withdraw(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "회원 탈퇴가 완료되었습니다"));
    }

    @Data
    static class ProfileReq {
        private String nickname;
    }
}
