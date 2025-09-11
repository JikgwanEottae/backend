package yagu.yagu.community.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.community.entity.CategoryType;
import yagu.yagu.community.entity.Post;
import yagu.yagu.user.entity.User;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 기본/카테고리별 페이징 조회 (이미지 사전 로딩)
    @EntityGraph(attributePaths = { "images" })
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = { "images" })
    Page<Post> findAllByCategory(CategoryType category, Pageable pageable);

    // 인기글: 좋아요 수 기준 내림차순 정렬 (이미지 사전 로딩)
    @EntityGraph(attributePaths = { "images" })
    @Query(value = "SELECT p FROM Post p LEFT JOIN p.likes l GROUP BY p ORDER BY COUNT(l) DESC", countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findPopular(Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Post p where p.owner = :owner")
    void deleteByOwner(@Param("owner") User owner);

    // 일괄 카운트 조회 (좋아요)
    @Query("SELECT p.id, COUNT(l) FROM Post p LEFT JOIN p.likes l WHERE p.id IN :postIds GROUP BY p.id")
    List<Object[]> countLikesByPostIds(@Param("postIds") Collection<Long> postIds);

    // 일괄 카운트 조회 (댓글)
    @Query("SELECT p.id, COUNT(c) FROM Post p LEFT JOIN p.comments c WHERE p.id IN :postIds GROUP BY p.id")
    List<Object[]> countCommentsByPostIds(@Param("postIds") Collection<Long> postIds);
}
