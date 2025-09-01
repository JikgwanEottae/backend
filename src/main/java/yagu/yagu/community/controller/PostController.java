package yagu.yagu.community.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.common.security.CustomOAuth2User;
import yagu.yagu.community.dto.*;
import yagu.yagu.community.entity.CategoryType;
import yagu.yagu.community.service.CommentLikeService;
import yagu.yagu.community.service.CommentService;
import yagu.yagu.community.service.LikeService;
import yagu.yagu.community.service.PostService;
import yagu.yagu.image.service.ImageService;
import yagu.yagu.user.entity.User;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final ImageService imageService;

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    // 게시글 생성 (멀티파트: dto + optional files)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @RequestPart("dto") @Valid PostRequestDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        // 1) 인증 체크
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        User user = principal.getUser();

        // 2) 파일 업로드는 서비스에서 처리하도록 DTO에 파일 URL 세팅 로직 제거

        // 3) 서비스 호출 (파일은 서비스에서 처리)
        if (files != null && !files.isEmpty()) {
            dto.setImageUrls(files.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(imageService::upload)
                    .collect(Collectors.toList()));
        }
        PostResponseDto response = postService.createPost(user, dto);

        // 4) Location 헤더 및 응답
        URI location = URI.create("/api/posts/" + response.getId());
        return ResponseEntity
                .created(location)
                .body(ApiResponse.created(response, "게시글 작성 완료"));
    }

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponseDto>>> listPosts(
            @RequestParam(value = "category", required = false) CategoryType category,
            @RequestParam(value = "popular", required = false, defaultValue = "false") boolean popular,
            Pageable pageable) {
        Page<PostResponseDto> list = postService.listPosts(category, popular, pageable);
        return ResponseEntity.ok(ApiResponse.success(list, "게시글 목록 조회 완료"));
    }

    // 게시글 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPost(@PathVariable Long id) {
        PostResponseDto dto = postService.getPost(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "게시글 상세 조회 완료"));
    }

    // 게시글 수정 (멀티파트: dto + optional files)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostResponseDto>> updatePost(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id,
            @RequestPart("dto") @Valid PostRequestDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        log.info("updatePost 호출: dto={}, filesCount={}", dto, files == null ? 0 : files.size());

        // 1) 인증 체크
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        User user = principal.getUser();

        // 2) 파일 업로드는 서비스에서 처리하도록 DTO에 파일 URL 세팅 로직 제거

        // 3) 서비스 호출 (파일 업로드 후 델타 반영)
        if (files != null && !files.isEmpty()) {
            List<String> added = files.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(imageService::upload)
                    .collect(Collectors.toList());
            List<String> keep = Optional.ofNullable(dto.getImageUrls()).orElse(List.of());
            List<String> merged = new java.util.ArrayList<>(keep);
            merged.addAll(added);
            dto.setImageUrls(merged);
        }
        PostResponseDto updated = postService.updatePost(user, id, dto);

        // 4) 응답
        return ResponseEntity.ok(ApiResponse.success(updated, "게시글 수정 완료"));
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        postService.deletePost(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    // 좋아요 누르기
    @PostMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> likePost(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        LikeResponseDto res = likeService.like(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success(res, "좋아요 완료"));
    }

    // 좋아요 취소
    @DeleteMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> unlikePost(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        LikeResponseDto res = likeService.unlike(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success(res, "좋아요 취소 완료"));
    }

    // 좋아요 상태 조회
    @GetMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> getLikeStatus(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        LikeResponseDto res = likeService.status(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success(res, "좋아요 상태 조회 완료"));
    }

    // 댓글 작성
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponseDto>> createComment(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long id,
            @RequestBody @Valid CommentRequestDto dto) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        CommentResponseDto res = commentService.create(principal.getUser(), id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(res, "댓글 작성 완료"));
    }

    // 댓글 목록 조회
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> listComments(
            @PathVariable Long id) {
        List<CommentResponseDto> list = commentService.list(id);
        return ResponseEntity.ok(ApiResponse.success(list, "댓글 목록 조회 완료"));
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long commentId) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        commentService.delete(principal.getUser(), commentId);
        return ResponseEntity.noContent().build();
    }

    // 댓글 좋아요 누르기
    @PostMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> likeComment(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long commentId) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        LikeResponseDto res = commentLikeService.like(principal.getUser(), commentId);
        return ResponseEntity.ok(ApiResponse.success(res, "댓글 좋아요 완료"));
    }

    // 댓글 좋아요 취소
    @DeleteMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> unlikeComment(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long commentId) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        LikeResponseDto res = commentLikeService.unlike(principal.getUser(), commentId);
        return ResponseEntity.ok(ApiResponse.success(res, "댓글 좋아요 취소 완료"));
    }

    // 댓글 좋아요 상태 조회
    @GetMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> getCommentLikeStatus(
            @AuthenticationPrincipal CustomOAuth2User principal,
            @PathVariable Long commentId) {
        if (principal == null || principal.getUser() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        LikeResponseDto res = commentLikeService.status(principal.getUser(), commentId);
        return ResponseEntity.ok(ApiResponse.success(res, "댓글 좋아요 상태 조회 완료"));
    }
}
