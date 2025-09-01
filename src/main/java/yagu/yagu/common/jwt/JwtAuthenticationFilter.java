package yagu.yagu.common.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        return p.startsWith("/api/auth/login/failure")
                || p.startsWith("/swagger-ui/") || p.startsWith("/v3/api-docs") || p.startsWith("/api/auth/refresh");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = jwtProvider.resolveToken(req);
        if (token != null && jwtProvider.validateToken(token)) {
            try {
                Authentication auth = jwtProvider.getAuthentication(token);
                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (UsernameNotFoundException ex) {
                // 유저가 더 이상 존재하지 않으면 인증 없이 계속 진행
            } catch (RuntimeException ex) {
                // 기타 토큰 관련 예외는 인증 없이 계속 진행
            }
        }
        chain.doFilter(req, res);
    }
}
