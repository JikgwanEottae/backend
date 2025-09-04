package yagu.yagu.common.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KakaoApiClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoApiClient.class);
    private static final String USERINFO = "https://kapi.kakao.com/v2/user/me";
    private final RestClient rest = RestClient.create();

    public KakaoUser me(String accessToken) {
        try {
            Map<?, ?> body = rest.get()
                    .uri(USERINFO)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (body == null || body.get("id") == null) {
                throw new IllegalStateException("Kakao userinfo empty: " + body);
            }

            String id = String.valueOf(body.get("id"));
            Map<?, ?> kakaoAccount = (Map<?, ?>) body.get("kakao_account");
            Map<?, ?> props        = (Map<?, ?>) body.get("properties");

            String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            String nick  = props != null ? (String) props.get("nickname") : null;
            if (nick == null && email != null) nick = email.split("@")[0];
            if (email == null) email = "kakao_" + id + "@kakao.local";

            return new KakaoUser(id, email, (nick != null ? nick : "KakaoUser"));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("Kakao /v2/user/me failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            // 401/403 등은 인증 실패로 처리
            throw new RuntimeException("KAKAO_TOKEN_INVALID");
        } catch (Exception e) {
            log.error("Kakao /v2/user/me error", e);
            throw new RuntimeException("KAKAO_API_ERROR");
        }
    }

    public record KakaoUser(String id, String email, String nickname) {}
}