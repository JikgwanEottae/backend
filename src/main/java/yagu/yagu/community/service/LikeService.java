package yagu.yagu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.community.dto.LikeResponseDto;
import yagu.yagu.community.entity.Post;
import yagu.yagu.community.entity.PostLike;
import yagu.yagu.community.repository.PostLikeRepository;
import yagu.yagu.community.repository.PostRepository;
import yagu.yagu.user.entity.User;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final PostLikeRepository likeRepo;
    private final PostRepository postRepo;

    @Transactional
    public LikeResponseDto like(User owner, Long postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
        if (likeRepo.findByPostAndOwner(post, owner).isPresent()) {
            throw new IllegalArgumentException("이미 좋아요를 눌렀습니다.");
        }
        likeRepo.save(PostLike.builder().post(post).owner(owner).build());
        Long count = likeRepo.countByPost(post);
        return LikeResponseDto.builder()
                .likeCount(count)
                .likedByCurrentUser(true)
                .build();
    }

    @Transactional
    public LikeResponseDto unlike(User owner, Long postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
        PostLike existing = likeRepo.findByPostAndOwner(post, owner)
                .orElseThrow(() -> new IllegalArgumentException("좋아요를 누른 적이 없습니다."));
        likeRepo.delete(existing);
        Long count = likeRepo.countByPost(post);
        return LikeResponseDto.builder()
                .likeCount(count)
                .likedByCurrentUser(false)
                .build();
    }

    @Transactional(readOnly = true)
    public LikeResponseDto status(User owner, Long postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
        Long count = likeRepo.countByPost(post);
        boolean liked = likeRepo.findByPostAndOwner(post, owner).isPresent();
        return LikeResponseDto.builder()
                .likeCount(count)
                .likedByCurrentUser(liked)
                .build();
    }
}

