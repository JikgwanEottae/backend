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
import yagu.yagu.community.repository.PostImageRepository;
import yagu.yagu.community.repository.PostRepository;
import yagu.yagu.user.entity.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepo;
    private final PostImageRepository imageRepo;

    @Transactional
    public PostResponseDto createPost(User owner, PostRequestDto dto) {
        // 1) Post 생성 및 저장
        Post post = Post.create(dto.getTitle(), dto.getContent(), dto.getCategory(), owner);
        Post saved = postRepo.save(post);

        // 2) 이미지가 있을 때 처리
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> imgs = dto.getImageUrls().stream()
                    .map(PostImage::of)
                    .collect(Collectors.toList());

            // 3) 연관관계 설정
            imgs.forEach(img -> img.assignToPost(saved));

            // 4) DB 저장
            imageRepo.saveAll(imgs);
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> listPosts(CategoryType category, boolean popular) {
        List<Post> posts;
        if (popular) {
            posts = postRepo.findPopular();
        } else if (category == null) {
            posts = postRepo.findAll();
        } else {
            posts = postRepo.findAllByCategory(category);
        }
        return posts.stream().map(this::mapToDto).collect(Collectors.toList());
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

        // 3) 기존 이미지 삭제
        imageRepo.deleteAll(post.getImages());
        post.getImages().clear();

        // 4) 새 이미지 처리
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> imgs = dto.getImageUrls().stream()
                    .map(PostImage::of)
                    .collect(Collectors.toList());

            imgs.forEach(img -> img.assignToPost(post));
            imageRepo.saveAll(imgs);
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
        postRepo.delete(post);
    }

    private PostResponseDto mapToDto(Post post) {
        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .collect(Collectors.toList());

        long likeCount = post.getLikes().size();
        long commentCount = post.getComments().size();

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
}
