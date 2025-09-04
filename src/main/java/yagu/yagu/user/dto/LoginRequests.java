package yagu.yagu.user.dto;

public class LoginRequests {
    public record KakaoLoginRequest(String accessToken, String nickname) {}
    public record AppleLoginRequest(String identityToken, String nonce, String nickname) {}
}
