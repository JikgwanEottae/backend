package yagu.yagu.common.security;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import yagu.yagu.user.entity.User;

import java.util.Map;

public class CustomOidcUser extends CustomOAuth2User implements OidcUser {
    private final OidcUser oidcUser;

    public CustomOidcUser(User user, OidcUser oidcUser) {
        super(user, oidcUser.getAttributes());
        this.oidcUser = oidcUser;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }
}

