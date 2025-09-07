package yagu.yagu.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.service.AuthService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final AuthService authService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {

        OidcUser delegate = super.loadUser(userRequest);

        Map<String, Object> attrs = delegate.getAttributes();
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google"
        String sub = delegate.getSubject();
        String email = delegate.getEmail();
        if (email == null) {
            Object v = attrs.get("email");
            if (v != null) email = v.toString();
        }
        String name = (String) attrs.getOrDefault("name", email);


        User user = authService.findOrCreateByProvider(
                User.AuthProvider.valueOf(provider.toUpperCase()),
                sub,
                email,
                (name != null ? name : (email != null ? email.split("@")[0] : "User"))
        );


        return new CustomOidcUser(
                user,
                attrs,
                delegate.getAuthorities(),
                delegate.getIdToken(),
                delegate.getUserInfo()
        );
    }
}