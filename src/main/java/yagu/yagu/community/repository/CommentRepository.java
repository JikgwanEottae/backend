package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.community.entity.Comment;
import yagu.yagu.community.entity.Post;
import yagu.yagu.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostAndParentCommentIsNull(Post post);

    Optional<Comment> findByIdAndOwner(Long id, User owner);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.owner = :owner")
    void deleteByOwner(@Param("owner") User owner);

    // 내 글(Post)에 달린 모든 댓글 (타인 댓글 포함)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.post.owner = :owner")
    void deleteByPostOwner(@Param("owner") User owner);
}
