package yagu.yagu.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
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
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User oAuth2 = super.loadUser(req);
        String provider = req.getClientRegistration().getRegistrationId();
        Map<String,Object> a = oAuth2.getAttributes();

        // 이메일
        String email = switch(provider) {
            case "google" -> (String) a.get("email");
            case "kakao"  -> (String)((Map<?,?>)a.get("kakao_account")).get("email");
            case "apple"  -> (String) a.get("email");
            default -> throw new RuntimeException("지원 안 함");
        };
        // 닉네임
        String nick = switch(provider) {
            case "google" -> (String) a.get("name");
            case "kakao"  -> (String)((Map<?,?>)a.get("properties")).get("nickname");
            case "apple"  -> (String) a.get("name");
            default -> email;
        };
        // 프로바이더 ID
        String pid = switch(provider) {
            case "google","apple" -> (String) a.get("sub");
            case "kakao"         -> String.valueOf(a.get("id"));
            default -> null;
        };

        User user = authService.findOrCreateUser(
                email, nick,
                User.AuthProvider.valueOf(provider.toUpperCase()),
                pid
        );
        return new CustomOAuth2User(user, a);
    }
}
