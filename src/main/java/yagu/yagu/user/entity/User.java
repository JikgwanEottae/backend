package yagu.yagu.user.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public enum AuthProvider { GOOGLE, KAKAO, APPLE }


    public void completeProfile(String nickname) {
        this.nickname = nickname;
        this.profileCompleted = true;
    }
}
