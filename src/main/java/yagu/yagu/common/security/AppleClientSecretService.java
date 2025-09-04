package yagu.yagu.common.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class AppleClientSecretService {

    private final String teamId   = getenvRequired("APPLE_TEAM_ID");
    private final String keyId    = getenvRequired("APPLE_KEY_ID");
    private final String clientId = getenvRequired("APPLE_CLIENT_ID");
    private final String p8Base64 = getenvRequired("APPLE_P8_B64");

    private volatile String cached;
    private volatile long   cachedExpEpochSec = 0L;

    public synchronized String getClientSecret() {
        long now = Instant.now().getEpochSecond();
        if (cached != null && (cachedExpEpochSec - now) > (60L * 60L * 24L * 30L)) {
            return cached;
        }
        try {
            byte[] der = Base64.getDecoder().decode(p8Base64);
            var spec  = new PKCS8EncodedKeySpec(der);
            var kf    = KeyFactory.getInstance("EC");
            ECPrivateKey ecPrivateKey = (ECPrivateKey) kf.generatePrivate(spec);

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(keyId)
                    .type(JOSEObjectType.JWT)
                    .build();

            long iat = now;
            long exp = now + 60L * 60L * 24L * 180L; // 180일(최대 6개월)

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(teamId)
                    .subject(clientId)
                    .audience("https://appleid.apple.com")
                    .issueTime(new Date(iat * 1000))
                    .expirationTime(new Date(exp * 1000))
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            jwtsSign(jwt, ecPrivateKey);

            cached = jwt.serialize();
            cachedExpEpochSec = exp;
            return cached;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Apple client_secret", e);
        }
    }

    private static void jwtsSign(SignedJWT jwt, ECPrivateKey pk) throws JOSEException {
        JWSSigner signer = new ECDSASigner(pk);
        jwt.sign(signer);
    }

    private static String getenvRequired(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing ENV: " + key);
        }
        return v.trim();
    }
}