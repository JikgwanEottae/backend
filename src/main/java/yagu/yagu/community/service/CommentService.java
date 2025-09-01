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
                                                "게시글이 없습니다. id=" + postId));

                Long parentId = dto.getParentCommentId();
                Comment parent = null;
                if (parentId != null && parentId > 0) {
                        parent = commentRepo.findById(parentId)
                                        .orElseThrow(() -> new BusinessException(
                                                        ErrorCode.NOT_FOUND,
                                                        "부모 댓글이 없습니다. id=" + parentId));
                        // 부모 댓글이 동일 게시글에 속하는지 검증
                        if (!parent.getPost().getId().equals(post.getId())) {
                                throw new BusinessException(
                                                ErrorCode.INVALID_REQUEST,
                                                "부모 댓글이 해당 게시글에 속하지 않습니다. postId=" + postId + ", parentId=" + parentId);
                        }
                }

                // Builder 대신 생성자 호출
                Comment comment = new Comment(
                                dto.getContent(),
                                owner,
                                post,
                                parent);

                Comment saved = commentRepo.save(comment);
                return mapToDtoWithReplies(saved);
        }

        @Transactional(readOnly = true)
        public List<CommentResponseDto> list(Long postId) {
                Post post = postRepo.findById(postId)
                                .orElseThrow(() -> new BusinessException(
                                                ErrorCode.NOT_FOUND,
                                                "게시글이 없습니다. id=" + postId));
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
                                                "댓글이 없거나 권한이 없습니다. id=" + commentId));
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
