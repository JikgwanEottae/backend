package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.community.entity.PostImage;
import yagu.yagu.user.entity.User;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PostImage pi where pi.post.owner = :owner")
    void deleteByPostOwner(@Param("owner") User owner);
}

