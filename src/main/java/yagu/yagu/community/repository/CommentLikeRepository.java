package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.community.entity.Comment;
import yagu.yagu.community.entity.CommentLike;
import yagu.yagu.user.entity.User;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentAndOwner(Comment comment, User owner);

    Long countByComment(Comment comment);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentLike cl where cl.owner = :owner")
    void deleteByOwner(@Param("owner") User owner);

    // 내가 쓴 댓글에 달린 좋아요
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentLike cl where cl.comment.owner = :owner")
    void deleteByCommentOwner(@Param("owner") User owner);

    // 내 글에 달린 모든 댓글의 좋아요 (타인 댓글 포함)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommentLike cl where cl.comment.post.owner = :owner")
    void deleteByCommentPostOwner(@Param("owner") User owner);
}
