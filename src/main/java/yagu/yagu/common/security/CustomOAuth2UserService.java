package yagu.yagu.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.service.AuthService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AuthService authService;
    private final OidcUserService oidcDelegate = new OidcUserService();

    // 1) 카카오·그 외 OAuth2 로그인 처리
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oauth2User = super.loadUser(request);
        return buildCustomUser(
                oauth2User.getAttributes(),
                request.getClientRegistration().getRegistrationId()
        );
    }

    // 2) 구글·애플 OIDC 로그인 처리
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser oidcUser = oidcDelegate.loadUser(request);
        return (OidcUser) buildCustomUser(
                oidcUser.getAttributes(),
                request.getClientRegistration().getRegistrationId()
        );
    }

    // 공통 래핑 로직
    private CustomOAuth2User buildCustomUser(Map<String, Object> attrs, String provider) {
        String email = switch (provider) {
            case "google", "apple" -> (String) attrs.get("email");
            case "kakao"           -> (String) ((Map<?, ?>) attrs.get("kakao_account")).get("email");
            default                -> throw new IllegalArgumentException("지원하지 않는 프로바이더: " + provider);
        };
        String nick = switch (provider) {
            case "google", "apple" -> (String) attrs.get("name");
            case "kakao"           -> (String) ((Map<?, ?>) attrs.get("properties")).get("nickname");
            default                -> email;
        };
        String pid = switch (provider) {
            case "google", "apple" -> (String) attrs.get("sub");
            case "kakao"           -> String.valueOf(attrs.get("id"));
            default                -> null;
        };

        User user = authService.findOrCreateUser(
                email,
                nick,
                User.AuthProvider.valueOf(provider.toUpperCase()),
                pid
        );
        return new CustomOAuth2User(user, attrs);
    }
}
