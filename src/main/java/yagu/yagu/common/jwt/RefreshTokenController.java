package yagu.yagu.common.jwt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
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
                HttpServletResponse res) {

                // 1) 토큰 추출
                String token = cookieToken != null ? cookieToken : (body != null ? body.get("refreshToken") : null);
                if (token == null || token.isBlank()) {
                        // 400: 요청 자체가 불완전
                        throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING);
                }

                // 2) DB 조회
                RefreshToken stored;
                try {
                        stored = refreshService.findByToken(token);
                } catch (RuntimeException e) {
                        // 401: 유효하지 않은 토큰(존재하지 않음/쓰레기값)
                        throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
                }

                // 3) 만료 검사
                try {
                        refreshService.verifyExpiration(stored);
                } catch (RuntimeException e) {
                        // 419: 만료
                        throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
                }

                // 4) 탈퇴 계정 차단
                if (stored.getUser().isDeleted()) {
                        refreshService.deleteByUser(stored.getUser());
                        // 403: 탈퇴 계정
                        throw new BusinessException(ErrorCode.USER_DELETED_FORBIDDEN);
                }

                // 5) 새 AT/RT 발급 & 쿠키 갱신
                String newAccess = jwtProvider.createToken(stored.getUser().getEmail());
                RefreshToken newRefresh = refreshService.createRefreshToken(stored.getUser());

                ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh.getToken())
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(jwtConfig.getRefreshExpiration() / 1000)
                        .build();
                res.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                return ResponseEntity.ok(
                        ApiResponse.success(Map.of("accessToken", newAccess), "액세스 토큰 재발급 완료")
                );
        }
}
