package yagu.yagu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.community.dto.LikeResponseDto;
import yagu.yagu.community.entity.Comment;
import yagu.yagu.community.entity.CommentLike;
import yagu.yagu.community.repository.CommentLikeRepository;
import yagu.yagu.community.repository.CommentRepository;
import yagu.yagu.user.entity.User;

@Service
@RequiredArgsConstructor
public class CommentLikeService {
    private final CommentLikeRepository likeRepo;
    private final CommentRepository commentRepo;

    @Transactional
    public LikeResponseDto like(User owner, Long commentId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다. id=" + commentId));
        if (likeRepo.findByCommentAndOwner(comment, owner).isPresent()) {
            throw new IllegalArgumentException("이미 좋아요했습니다.");
        }
        likeRepo.save(CommentLike.builder().comment(comment).owner(owner).build());
        return buildDto(comment, true);
    }

    @Transactional
    public LikeResponseDto unlike(User owner, Long commentId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다. id=" + commentId));
        CommentLike existing = likeRepo.findByCommentAndOwner(comment, owner)
                .orElseThrow(() -> new IllegalArgumentException("좋아요를 누르지 않았습니다."));
        likeRepo.delete(existing);
        return buildDto(comment, false);
    }

    @Transactional(readOnly = true)
    public LikeResponseDto status(User owner, Long commentId) {
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 없습니다. id=" + commentId));
        return buildDto(comment, likeRepo.findByCommentAndOwner(comment, owner).isPresent());
    }

    private LikeResponseDto buildDto(Comment comment, boolean liked) {
        Long count = likeRepo.countByComment(comment);
        return LikeResponseDto.builder()
                .likeCount(count)
                .likedByCurrentUser(liked)
                .build();
    }
}

