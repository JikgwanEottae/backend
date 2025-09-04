package yagu.yagu.common.oauth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth.apple")
public class AppleProperties {
    private List<String> clientIds;
    private String issuer   = "https://appleid.apple.com";
    private String jwksUri  = "https://appleid.apple.com/auth/keys";
}