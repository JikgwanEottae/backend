package yagu.yagu.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.service.AuthService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);
        Map<String, Object> attrs = oauth2User.getAttributes();

        String provider = request.getClientRegistration().getRegistrationId();
        // 구글 기준으로 파싱
        String email = (String) attrs.get("email");
        String name  = (String) attrs.getOrDefault("name", email);
        String sub   = (String) attrs.get("sub");

        User user = authService.findOrCreateByProvider(
                User.AuthProvider.valueOf(provider.toUpperCase()),
                sub,
                email,
                name
        );

        return new CustomOAuth2User(user, attrs);
    }
}
