package yagu.yagu.community.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import yagu.yagu.common.response.ApiResponse;
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

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<PostResponseDto>> createPost(
            @AuthenticationPrincipal User user,
            @RequestPart("dto") PostRequestDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        if (files != null && !files.isEmpty()) {
            List<String> urls = files.stream()
                    .map(imageService::upload)
                    .collect(Collectors.toList());
            dto.setImageUrls(urls);
        }
        PostResponseDto response = postService.createPost(user, dto);
        URI location = URI.create("/api/posts/" + response.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.created(response, "게시글 작성 완료"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostResponseDto>>> listPosts(
            @RequestParam(value = "category", required = false) CategoryType category
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.listPosts(category)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponseDto>> updatePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestPart("dto") PostRequestDto dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        if (files != null && !files.isEmpty()) {
            List<String> urls = files.stream()
                    .map(imageService::upload)
                    .collect(Collectors.toList());
            dto.setImageUrls(urls);
        }
        PostResponseDto updated = postService.updatePost(user, id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        postService.deletePost(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> likePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(likeService.like(user, id)));
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> unlikePost(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(likeService.unlike(user, id)));
    }

    @GetMapping("/{id}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> getLikeStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(likeService.status(user, id)));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentResponseDto>> createComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody CommentRequestDto dto
    ) {
        CommentResponseDto created = commentService.create(user, id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponseDto>>> listComments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commentService.list(id)));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId
    ) {
        commentService.delete(user, commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> likeComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentLikeService.like(user, commentId)));
    }

    @DeleteMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> unlikeComment(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentLikeService.unlike(user, commentId)));
    }

    @GetMapping("/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponseDto>> getCommentLikeStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentLikeService.status(user, commentId)));
    }
}
