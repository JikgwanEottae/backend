package yagu.yagu.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import yagu.yagu.user.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User, UserDetails {
    private final User user;
    private final Map<String, Object> attrs;

    public CustomOAuth2User(User user, Map<String, Object> attrs) {
        this.user = user;
        this.attrs = attrs;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attrs;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // UserDetails 인터페이스 메서드
    @Override public String getPassword()               { return null; }
    @Override public String getUsername()               { return user.getEmail(); }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return true; }

    // OAuth2User 인터페이스 메서드
    @Override
    public String getName() {
        return user.getEmail();
    }
}