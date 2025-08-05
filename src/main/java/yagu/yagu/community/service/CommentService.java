package yagu.yagu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.community.dto.CommentRequestDto;
import yagu.yagu.community.dto.CommentResponseDto;
import yagu.yagu.community.entity.Comment;
import yagu.yagu.community.entity.Post;
import yagu.yagu.community.repository.CommentRepository;
import yagu.yagu.community.repository.PostRepository;
import yagu.yagu.user.entity.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepo;
    private final PostRepository postRepo;

    @Transactional
    public CommentResponseDto create(User owner, Long postId, CommentRequestDto dto) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "게시글이 없습니다. id=" + postId
                ));
        Comment parent = null;
        if (dto.getParentCommentId() != null) {
            parent = commentRepo.findById(dto.getParentCommentId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.NOT_FOUND,
                            "부모 댓글이 없습니다. id=" + dto.getParentCommentId()
                    ));
        }
        Comment comment = Comment.builder()
                .content(dto.getContent())
                .owner(owner)
                .post(post)
                .parentComment(parent)
                .build();
        Comment saved = commentRepo.save(comment);
        return mapToDtoWithReplies(saved);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> list(Long postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "게시글이 없습니다. id=" + postId
                ));
        return commentRepo.findAllByPostAndParentCommentIsNull(post)
                .stream()
                .map(this::mapToDtoWithReplies)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(User owner, Long commentId) {
        Comment comment = commentRepo.findByIdAndOwner(commentId, owner)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OPERATION_DENIED,
                        "댓글이 없거나 권한이 없습니다. id=" + commentId
                ));
        commentRepo.delete(comment);
    }

    private CommentResponseDto mapToDtoWithReplies(Comment c) {
        CommentResponseDto dto = mapToDto(c);
        List<CommentResponseDto> replies = c.getReplies().stream()
                .map(this::mapToDtoWithReplies)
                .collect(Collectors.toList());
        dto.setReplies(replies);
        return dto;
    }

    private CommentResponseDto mapToDto(Comment c) {
        return CommentResponseDto.builder()
                .id(c.getId())
                .content(c.getContent())
                .ownerId(c.getOwner().getId())
                .ownerNickname(c.getOwner().getNickname())
                .replies(null)
                .build();
    }
}

