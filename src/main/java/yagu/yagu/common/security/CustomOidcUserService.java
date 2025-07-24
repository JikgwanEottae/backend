package yagu.yagu.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.service.AuthService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OidcUserRequest, OidcUser> {
    private final AuthService authService;
    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(request);
        Map<String, Object> attrs = oidcUser.getAttributes();
        String provider = request.getClientRegistration().getRegistrationId();

        String email = (String) attrs.get("email");
        String nick  = (String) attrs.getOrDefault("name", email);
        String pid   = (String) attrs.get("sub");

        User user = authService.findOrCreateUser(
                email,
                nick,
                User.AuthProvider.valueOf(provider.toUpperCase()),
                pid
        );
        return new CustomOidcUser(user, oidcUser);
    }
}
