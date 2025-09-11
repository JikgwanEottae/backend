package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.community.entity.Post;
import yagu.yagu.community.entity.PostLike;
import yagu.yagu.user.entity.User;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndOwner(Post post, User owner);

    Long countByPost(Post post);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostLike pl where pl.owner = :owner")
    void deleteByOwner(@Param("owner") User owner);

    // 내 글에 달린 모든 좋아요 제거 (타인 좋아요 포함)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostLike pl where pl.post.owner = :owner")
    void deleteByPostOwner(@Param("owner") User owner);
}
