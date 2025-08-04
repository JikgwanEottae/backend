package yagu.yagu.common.jwt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.response.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RefreshTokenController {
    private final RefreshTokenService refreshService;
    private final JwtTokenProvider jwtProvider;
    private final JwtConfig jwtConfig;

    /**
     * 웹에서는 HttpOnly 쿠키에서 모바일에서는 JSON 바디에서
     * refreshToken을 꺼내어 액세스 토큰을 재발급합니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String cookieToken,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse res
    ) {
        // 1) 쿠키에 토큰이 없으면 바디에서 꺼내고
        String token = cookieToken != null
                ? cookieToken
                : (body != null ? body.get("refreshToken") : null);

        if (token == null) {
            ApiResponse<Map<String,String>> error = ApiResponse.<Map<String,String>>builder()
                    .result(false)
                    .httpCode(HttpStatus.UNAUTHORIZED.value())
                    .data(null)
                    .message("Refresh token is missing")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // 2) DB에서 토큰 조회·만료 검사
        RefreshToken stored = refreshService.findByToken(token);
        refreshService.verifyExpiration(stored);

        // 3) 새 액세스 토큰 발급
        String newAccess = jwtProvider.createToken(stored.getUser().getEmail());
        // 4) 토큰 회전: 새 리프레시 토큰 발급
        RefreshToken newRefresh = refreshService.createRefreshToken(stored.getUser());

        // 5) (웹용) HttpOnly 쿠키로도 갱신
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtConfig.getRefreshExpiration() / 1000)
                .build();
        res.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 6) JSON 응답: 새 액세스 토큰만
        Map<String, String> data = Map.of("accessToken", newAccess);
        ApiResponse<Map<String,String>> success =
                ApiResponse.success(data, "액세스 토큰 재발급 완료");

        return ResponseEntity.ok(success);
    }
}
