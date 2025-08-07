package yagu.yagu.community.entity;

import jakarta.persistence.*;
import lombok.*;
import yagu.yagu.user.entity.User;

@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "owner_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    /** 생성자: 필수 필드만 */
    public CommentLike(Comment comment, User owner) {
        this.comment = comment;
        this.owner = owner;
    }

    /** 정적 팩토리 */
    public static CommentLike of(Comment comment, User owner) {
        return new CommentLike(comment, owner);
    }
}

