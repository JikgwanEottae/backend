package yagu.yagu.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

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

    // 소프트 삭제/영구삭제 스케줄링을 위한 필드
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "purge_at")
    private Instant purgeAt;

    // 삭제 전 개인정보 스냅샷 (복구용)
    @Column(name = "deleted_original_email")
    private String deletedOriginalEmail;

    @Column(name = "deleted_original_nickname")
    private String deletedOriginalNickname;

    @Column(name = "deleted_original_profile_image_url")
    private String deletedOriginalProfileImageUrl;

    @Column(name = "apple_refresh_token", length = 512)
    private String appleRefreshToken;

    public void updateAppleRefreshToken(String rt) {
        this.appleRefreshToken = rt;
    }

    public String getAppleRefreshToken() {
        return appleRefreshToken;
    }

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

    /**
     * 소프트 삭제 및 익명화 처리. 이메일은 유니크 제약을 위해 대체 주소로 교체한다.
     * 닉네임은 "탈퇴회원-<id>" 형태로 치환하고 프로필 이미지는 제거한다.
     * deletedAt, purgeAt 을 설정한다.
     */
    public void anonymizeAndMarkDeleted(Instant deletedAt, Instant purgeAt) {
        // 삭제 전 개인정보 스냅샷 저장
        this.deletedOriginalEmail = this.email;
        this.deletedOriginalNickname = this.nickname;
        this.deletedOriginalProfileImageUrl = this.profileImageUrl;

        String suffix = this.id == null ? String.valueOf(System.currentTimeMillis()) : String.valueOf(this.id);
        this.email = "deleted-" + suffix + "@deleted.local";
        this.nickname = "탈퇴회원-" + suffix;
        this.profileImageUrl = null;
        this.profileCompleted = false;
        this.deletedAt = deletedAt;
        this.purgeAt = purgeAt;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 탈퇴 복구: 30일 내 재로그인 시 기존 계정 복구하여 기존 데이터(게시글/댓글 등)를 그대로 사용.
     */
    public void restoreFromDeletion(String latestEmailFromProvider, String defaultNicknameFromProvider) {
        this.email = latestEmailFromProvider;
        this.nickname = (this.deletedOriginalNickname != null) ? this.deletedOriginalNickname
                : defaultNicknameFromProvider;
        this.profileImageUrl = this.deletedOriginalProfileImageUrl;
        this.deletedAt = null;
        this.purgeAt = null;
        this.deletedOriginalEmail = null;
        this.deletedOriginalNickname = null;
        this.deletedOriginalProfileImageUrl = null;
    }

    public void linkProvider(AuthProvider provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }
}
