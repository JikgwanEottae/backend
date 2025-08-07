package yagu.yagu.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(nullable = false)
    private String providerId;

    @Column
    private String profileImageUrl;

    @Column(nullable = false)
    private boolean profileCompleted;

    public enum AuthProvider {
        GOOGLE, KAKAO, APPLE
    }

    public User(String email, String nickname,
            AuthProvider provider, String providerId) {
        this.email = email;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.profileCompleted = false;
    }

    public static User of(String email, String nickname,
            AuthProvider provider, String providerId) {
        return new User(email, nickname, provider, providerId);
    }

    public void completeProfile(String nickname) {
        this.nickname = nickname;
        this.profileCompleted = true;
    }

    public void updateProfileImage(String url) {
        this.profileImageUrl = url;
    }
}
