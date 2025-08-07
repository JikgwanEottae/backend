package yagu.yagu.community.entity;

import jakarta.persistence.*;
import lombok.*;
import yagu.yagu.user.entity.User;

@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "owner_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    /** 생성자 */
    public PostLike(Post post, User owner) {
        this.post = post;
        this.owner = owner;
    }

    /** 정적 팩토리 */
    public static PostLike of(Post post, User owner) {
        return new PostLike(post, owner);
    }
}
