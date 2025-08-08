package yagu.yagu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.community.dto.PostRequestDto;
import yagu.yagu.community.dto.PostResponseDto;
import yagu.yagu.community.entity.CategoryType;
import yagu.yagu.community.entity.Post;
import yagu.yagu.community.entity.PostImage;
import yagu.yagu.community.repository.PostRepository;
import yagu.yagu.community.repository.PostImageRepository;
import yagu.yagu.user.entity.User;
import yagu.yagu.image.service.ImageService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepo;
    private final PostImageRepository imageRepo;
    private final ImageService imageService;

    @Transactional
    public PostResponseDto createPost(User owner, PostRequestDto dto) {
        // 1) Post 생성 및 저장
        Post post = Post.create(dto.getTitle(), dto.getContent(), dto.getCategory(), owner);
        Post saved = postRepo.save(post);

        // 2) 이미지 URL이 있을 때 처리 (컨트롤러에서 업로드 후 URL 설정)
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> imgs = dto.getImageUrls().stream()
                    .map(PostImage::of)
                    .collect(Collectors.toList());
            imgs.forEach(img -> img.assignToPost(saved));
            imageRepo.saveAll(imgs);
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> listPosts(CategoryType category, boolean popular, Pageable pageable) {
        Page<Post> page;
        if (popular) {
            page = postRepo.findPopular(pageable);
        } else if (category == null) {
            page = postRepo.findAll(pageable);
        } else {
            page = postRepo.findAllByCategory(category, pageable);
        }

        List<Long> postIds = page.getContent().stream().map(Post::getId).collect(Collectors.toList());
        Map<Long, Long> likeCountMap = postRepo.countLikesByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));
        Map<Long, Long> commentCountMap = postRepo.countCommentsByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]));

        return page.map(post -> mapToDto(post,
                likeCountMap.getOrDefault(post.getId(), 0L),
                commentCountMap.getOrDefault(post.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "게시글을 찾을 수 없습니다. id=" + id));
        return mapToDto(post);
    }

    @Transactional
    public PostResponseDto updatePost(User owner, Long id, PostRequestDto dto) {
        // 1) 권한 검사 및 조회
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다. id=" + id));
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_DENIED, "수정 권한이 없습니다. id=" + id);
        }

        // 2) 본문 수정
        post.update(dto.getTitle(), dto.getContent(), dto.getCategory());

        // 3) 이미지 델타 반영
        // 현재 DB의 이미지 URL 목록
        List<String> currentUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .collect(Collectors.toList());
        List<String> newUrls = dto.getImageUrls() == null ? List.of() : dto.getImageUrls();

        // 삭제 대상: current - new
        for (String url : currentUrls) {
            if (!newUrls.contains(url)) {
                imageService.deleteByUrl(url);
            }
        }
        // 엔티티 관계 갱신: 기존 모두 비우고 새 목록 재할당(orphans 제거)
        post.getImages().clear();
        if (!newUrls.isEmpty()) {
            List<PostImage> newImgs = newUrls.stream()
                    .map(PostImage::of)
                    .collect(Collectors.toList());
            newImgs.forEach(img -> img.assignToPost(post));
            imageRepo.saveAll(newImgs);
        }

        return mapToDto(post);
    }

    @Transactional
    public void deletePost(User owner, Long id) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "게시글을 찾을 수 없습니다. id=" + id));
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(
                    ErrorCode.OPERATION_DENIED,
                    "삭제 권한이 없습니다. id=" + id);
        }
        // 삭제 전 이미지 정리
        post.getImages().forEach(img -> imageService.deleteByUrl(img.getImageUrl()));
        postRepo.delete(post);
    }

    private PostResponseDto mapToDto(Post post, long likeCount, long commentCount) {
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .collect(Collectors.toList());

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .ownerId(post.getOwner().getId())
                .ownerNickname(post.getOwner().getNickname())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .imageUrls(imageUrls)
                .build();
    }

    private PostResponseDto mapToDto(Post post) {
        long likeCount = post.getLikes().size();
        long commentCount = post.getComments().size();
        return mapToDto(post, likeCount, commentCount);
    }
}
