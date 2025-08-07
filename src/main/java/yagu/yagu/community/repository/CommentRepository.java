package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.community.entity.Comment;
import yagu.yagu.community.entity.Post;
import yagu.yagu.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostAndParentCommentIsNull(Post post);

    Optional<Comment> findByIdAndOwner(Long id, User owner);
}
