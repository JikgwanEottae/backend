package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.community.entity.Post;
import yagu.yagu.community.entity.PostLike;
import yagu.yagu.user.entity.User;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndOwner(Post post, User owner);
    Long countByPost(Post post);
}

