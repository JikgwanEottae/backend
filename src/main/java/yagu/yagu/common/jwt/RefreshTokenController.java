package yagu.yagu.common.jwt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yagu.yagu.common.response.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RefreshTokenController {
    private final RefreshTokenService refreshService;
    private final JwtTokenProvider jwtProvider;
    private final JwtConfig jwtConfig;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String token,
            HttpServletResponse res
    ) {
        RefreshToken stored = refreshService.findByToken(token);
        refreshService.verifyExpiration(stored);

        // 새 액세스 토큰 발급
        String newAccess = jwtProvider.createToken(stored.getUser().getEmail());

        // 토큰 회전
        RefreshToken newRefresh = refreshService.createRefreshToken(stored.getUser());

        // 쿠키 갱신
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtConfig.getRefreshExpiration() / 1000)
                .build();
        res.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        Map<String, String> data = Map.of("accessToken", newAccess);
        ApiResponse<Map<String, String>> body = ApiResponse.success(data, "액세스 토큰 재발급 완료");

        return ResponseEntity.ok(body);
    }
}
