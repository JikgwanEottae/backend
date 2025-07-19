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
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        System.out.println("카카오 attributes: " + attributes);
        String email = getEmail(attributes, provider);
        String nickname = getNickname(attributes, provider);
        String providerId = getProviderId(attributes, provider);
        System.out.println("파싱 결과 - email: " + email + ", nickname: " + nickname + ", providerId: " + providerId);

        User user = authService.findOrCreateUser(
                email,
                nickname,
                User.AuthProvider.valueOf(provider.toUpperCase()),
                providerId
        );

        return new CustomOAuth2User(user, attributes);
    }

    private String getEmail(Map<String, Object> attributes, String provider) {
        if (provider.equals("kakao")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            return (String) kakaoAccount.get("email");
        } else if (provider.equals("apple")) {
            return (String) attributes.get("email");
        }
        return null;
    }

    private String getNickname(Map<String, Object> attributes, String provider) {
        if (provider.equals("kakao")) {
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            return (String) properties.get("nickname");
        } else if (provider.equals("apple")) {
            return (String) attributes.get("name");
        }
        return null;
    }

    private String getProviderId(Map<String, Object> attributes, String provider) {
        if (provider.equals("kakao")) {
            return String.valueOf(attributes.get("id"));
        } else if (provider.equals("apple")) {
            return (String) attributes.get("sub");
        }
        return null;
    }
}
