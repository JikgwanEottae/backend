package yagu.yagu.common.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AppleJwtVerifier {

    private final AppleProperties props;
    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public AppleJwtVerifier(AppleProperties props) throws Exception {
        this.props = props;
        var keySource = new RemoteJWKSet<SecurityContext>(new URL(props.getJwksUri()));
        var selector  = new JWSVerificationKeySelector<>(JWSAlgorithm.ES256, keySource);
        var p = new DefaultJWTProcessor<SecurityContext>();
        p.setJWSKeySelector(selector);
        this.processor = p;
    }

    public JWTClaimsSet verify(String identityToken) {
        try {
            var claims = processor.process(identityToken, null);

            // iss
            if (!props.getIssuer().equals(claims.getIssuer())) {
                throw new IllegalStateException("Invalid iss");
            }
            // aud
            List<String> aud = claims.getAudience();
            Set<String> allow = new HashSet<>(props.getClientIds());
            boolean ok = aud.stream().anyMatch(allow::contains);
            if (!ok) throw new IllegalStateException("Invalid aud");

            // exp
            if (claims.getExpirationTime() == null ||
                    claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new IllegalStateException("Expired token");
            }

            return claims;
        } catch (Exception e) {
            throw new IllegalStateException("Apple identity_token verify failed: " + e.getMessage(), e);
        }
    }

    public static String extractEmailSafe(JWTClaimsSet claims) {
        Object email = claims.getClaim("email");
        return email instanceof String ? (String) email : null;
    }
}