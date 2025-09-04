package yagu.yagu.common.oauth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleClientSecretService {

    @Value("${oauth.apple.team-id}")
    private String teamId;

    @Value("${oauth.apple.key-id}")
    private String keyId;

    // 보통 spring.security.oauth2.client.registration.apple.client-id 와 동일
    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    /** PEM 텍스트 or DER 바이트의 Base64 (예: APPLE_P8_B64 환경변수) */
    @Value("${oauth.apple.p8-b64}")
    private String p8Base64;

    private volatile String cached;
    private volatile long   cachedExpEpochSec = 0L;

    /** 최대 6개월(exp≤15777000초) 규정. 여기선 30일로 생성 후 캐시 */
    public synchronized String getClientSecret() {
        long now = Instant.now().getEpochSecond();

        // 만료 30일 이상 남았으면 캐시 재사용
        if (cached != null && (cachedExpEpochSec - now) > (60L * 60L * 24L * 30L)) {
            return cached;
        }

        try {
            ECPrivateKey ecPrivateKey = loadPrivateKeyFromEnv();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(keyId)
                    .type(JOSEObjectType.JWT)
                    .build();

            long iat = now;
            long exp = now + 60L * 60L * 24L * 30L; // 30일

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(teamId)                          // iss = Team ID
                    .subject(clientId)                        // sub = Service ID (클라이언트ID)
                    .audience("https://appleid.apple.com")    // aud 고정
                    .issueTime(new Date(iat * 1000))
                    .expirationTime(new Date(exp * 1000))
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            JWSSigner signer = new ECDSASigner(ecPrivateKey);
            jwt.sign(signer);

            cached = jwt.serialize();
            cachedExpEpochSec = exp;
            return cached;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Apple client_secret", e);
        }
    }

    /** p8Base64 가 'PEM 텍스트'의 Base64든, 'DER 바이트'의 Base64든 처리 */
    private ECPrivateKey loadPrivateKeyFromEnv() throws Exception {
        byte[] decoded = Base64.getDecoder().decode(p8Base64.trim());
        String asText = new String(decoded, StandardCharsets.US_ASCII);

        byte[] der;
        if (asText.contains("BEGIN PRIVATE KEY")) {
            String body = asText
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            der = Base64.getDecoder().decode(body);
        } else {
            der = decoded;
        }

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPrivateKey) kf.generatePrivate(spec);
    }
}