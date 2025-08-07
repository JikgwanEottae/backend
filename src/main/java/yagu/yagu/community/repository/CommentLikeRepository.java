package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.community.entity.Comment;
import yagu.yagu.community.entity.CommentLike;
import yagu.yagu.user.entity.User;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentAndOwner(Comment comment, User owner);

    Long countByComment(Comment comment);

    long deleteByOwner(User owner);
}
