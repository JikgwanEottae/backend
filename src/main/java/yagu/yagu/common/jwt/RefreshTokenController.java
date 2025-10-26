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

import java.util.LinkedHashMap;
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
        public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
                        @CookieValue(name = "refreshToken", required = false) String cookieToken,
                        @RequestBody(required = false) Map<String, String> body,
                        HttpServletResponse res) {

                // 1) 토큰 추출
                String token = cookieToken != null ? cookieToken : (body != null ? body.get("refreshToken") : null);
                if (token == null || token.isBlank()) {
                        throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISSING); // 400
                }

                // 2) DB 조회
                RefreshToken stored;
                try {
                        stored = refreshService.findByToken(token);
                } catch (RuntimeException e) {
                        throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID); // 401
                }

                // 3) 만료 검사(만료면 삭제 & 에러)
                try {
                        refreshService.verifyExpiration(stored);
                } catch (RuntimeException e) {
                        throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED); // 419
                }

                // 4) 탈퇴 계정 방어
                if (stored.getUser().isDeleted()) {
                        refreshService.deleteByUser(stored.getUser());
                        throw new BusinessException(ErrorCode.USER_DELETED_FORBIDDEN); // 403
                }

                // 5) ⬅️ 변경 포인트: 로테이션(새 RT 발급 + 기존 RT 삭제)
                RefreshToken rotated = refreshService.rotate(stored);

                // 6) 새 AT 발급
                String newAccess = jwtProvider.createToken(rotated.getUser().getEmail());

                // 7) 쿠키에 새 RT 저장(슬라이딩 만료 반영: now + refreshExpiration)
                ResponseCookie cookie = ResponseCookie.from("refreshToken", rotated.getToken())
                                .httpOnly(true).secure(true).path("/")
                                .maxAge(jwtConfig.getRefreshExpiration() / 1000) // 초 단위
                                .sameSite("None")
                                .build();
                res.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

                // 8) 응답 바디
                var u = rotated.getUser();
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("nickname", u.getNickname());
                data.put("profileImageUrl", u.getProfileImageUrl());
                data.put("favoriteTeam", u.getFavoriteTeam());
                data.put("accessToken", newAccess);
                data.put("refreshToken", rotated.getToken()); // ⬅️ 새 RT 반환

                return ResponseEntity.ok(ApiResponse.success(data, "액세스/리프레시 토큰 재발급 완료"));
        }
}
