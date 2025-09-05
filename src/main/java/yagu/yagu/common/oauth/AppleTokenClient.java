package yagu.yagu.common.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppleTokenClient {

    private final AppleClientSecretService clientSecretService;


    @Value("${oauth.apple.client-id}")
    private String clientId;

    private final RestClient http = RestClient.builder().build();

    /** authorization_code -> { id_token, access_token, refresh_token, ... } */
    public Map<String, Object> exchange(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecretService.getClientSecret());

        try {
            ResponseEntity<Map> resp = http.post()
                    .uri("https://appleid.apple.com/auth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(Map.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new RuntimeException("APPLE_TOKEN_EXCHANGE_FAILED: "
                        + resp.getStatusCode());
            }
            return resp.getBody();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Apple에서 401/400일 때 상세 body 확인
            throw new RuntimeException("APPLE_TOKEN_EXCHANGE_FAILED: "
                    + e.getStatusCode() + " body=" + e.getResponseBodyAsString(), e);
        }
    }

    /** refresh_token revoke */
    public void revokeRefreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecretService.getClientSecret());
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        http.post()
                .uri("https://appleid.apple.com/auth/revoke")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}