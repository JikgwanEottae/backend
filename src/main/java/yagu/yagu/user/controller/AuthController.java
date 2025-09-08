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
import yagu.yagu.common.oauth.AppleTokenClient;
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
    private final AppleTokenClient appleTokenClient;


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

        try {
            String idToken = req.identityToken();
            String refreshTokenFromApple = null;

            if (StringUtils.hasText(req.authorizationCode())) {
                Map<String, Object> tokenRes = appleTokenClient.exchange(req.authorizationCode());
                String exchangedIdToken = (String) tokenRes.get("id_token");
                if (StringUtils.hasText(exchangedIdToken)) {
                    // 교차검증
                    var c1 = appleJwtVerifier.verify(idToken);
                    var c2 = appleJwtVerifier.verify(exchangedIdToken);
                    if (!c1.getSubject().equals(c2.getSubject())) {
                        return ResponseEntity.badRequest().body(
                                ApiResponse.<Map<String,Object>>builder()
                                        .result(false).httpCode(400).data(null)
                                        .message("Apple 토큰 불일치").build()
                        );
                    }
                    idToken = exchangedIdToken;
                }
                refreshTokenFromApple = (String) tokenRes.get("refresh_token");
            }

            var claims = appleJwtVerifier.verify(idToken);
            String sub   = claims.getSubject();
            String email = AppleJwtVerifier.extractEmailSafe(claims);
            String fallback = (email != null) ? email.split("@")[0] : "AppleUser";
            String preferredNick = (req.nickname() != null && !req.nickname().isBlank())
                    ? req.nickname() : fallback;

            var user = authService.findOrCreateByProvider(User.AuthProvider.APPLE, sub, email, preferredNick);

            if (StringUtils.hasText(refreshTokenFromApple)) {
                user.updateAppleRefreshToken(refreshTokenFromApple);
                userRepo.save(user);
            }

            Map<String, Object> tokens = authService.createLoginResponse(user, res);
            return ResponseEntity.ok(ApiResponse.success(tokens, "로그인 성공"));

        } catch (RuntimeException ex) {
            String msg = ex.getMessage();
            int status = (msg != null && msg.startsWith("APPLE_TOKEN_EXCHANGE_FAILED")) ? 502 : 401;
            return ResponseEntity.status(status).body(
                    ApiResponse.<Map<String,Object>>builder()
                            .result(false).httpCode(status).data(null)
                            .message(status==502 ? "애플 토큰 교환 실패: " + msg
                                    : "애플 토큰 검증 실패: " + msg)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    ApiResponse.<Map<String,Object>>builder()
                            .result(false).httpCode(500).data(null)
                            .message("서버 오류")
                            .build()
            );
        }
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

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upsertProfile(
            @RequestBody ProfileReq req,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        if (!StringUtils.hasText(req.getNickname())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "닉네임을 입력해주세요.");
        }

        String nick = req.getNickname().trim();

        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();
        User user = principal.getUser();

        // 현재와 동일 닉네임이면 완료만 보장
        if (nick.equals(user.getNickname())) {
            if (!user.isProfileCompleted()) {
                user.completeProfile(nick);
                userRepo.save(user);
            }
            return ResponseEntity.ok(
                    ApiResponse.success(
                            Map.of("profileCompleted", true, "available", true),
                            "프로필 설정 완료"
                    )
            );
        }

        // 1차 중복 검사
        if (userRepo.existsByNickname(nick)) {
            return conflictNickname();
        }

        try {
            user.completeProfile(nick);
            userRepo.saveAndFlush(user); // DB 유니크 제약 위반 시 예외 발생
            return ResponseEntity.ok(
                    ApiResponse.success(
                            Map.of("profileCompleted", true, "available", true),
                            "프로필 설정 완료"
                    )
            );
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시성으로 같은 닉네임이 들어온 경우
            return conflictNickname();
        }
    }

    /** 닉네임 중복 409 응답 */
    private ResponseEntity<ApiResponse<Map<String, Object>>> conflictNickname() {
        return ResponseEntity.status(409).body(
                ApiResponse.<Map<String, Object>>builder()
                        .result(false)
                        .httpCode(409)
                        .data(Map.of("available", false))
                        .message("이미 사용 중인 닉네임입니다.")
                        .build()
        );
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

    /** 즉시 탈퇴 (복구 불가) */
    @DeleteMapping("/withdraw/immediate")
    public ResponseEntity<ApiResponse<Void>> withdrawImmediate(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        var principal = (CustomOAuth2User) authentication.getPrincipal();
        authService.immediateWithdraw(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(null, "즉시 탈퇴가 완료되었습니다(복구 불가)"));
    }

}
