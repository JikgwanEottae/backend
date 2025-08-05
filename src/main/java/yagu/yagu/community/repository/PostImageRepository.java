package yagu.yagu.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import yagu.yagu.community.entity.PostImage;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {}

