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

        // 이메일, 닉네임, providerId 추출 로직
        String email = (String) ((provider.equals("kakao"))
                ? ((Map<?, ?>) attrs.get("kakao_account")).get("email")
                : attrs.get("email"));
        String nick  = (String) ((provider.equals("kakao"))
                ? ((Map<?, ?>) attrs.get("properties")).get("nickname")
                : attrs.getOrDefault("name", email));
        String pid   = (String) String.valueOf(
                provider.equals("kakao") ? attrs.get("id") : attrs.get("sub")
        );

        User user = authService.findOrCreateUser(
                email,
                nick,
                User.AuthProvider.valueOf(provider.toUpperCase()),
                pid
        );
        return new CustomOAuth2User(user, attrs);
    }
}
