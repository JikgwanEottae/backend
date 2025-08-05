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
        Post post = Post.create(dto.getTitle(), dto.getContent(), dto.getCategory(), owner);
        Post saved = postRepo.save(post);

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> imgs = dto.getImageUrls().stream()
                    .map(PostImage::of) // Builder 대신 팩토리
                    .collect(Collectors.toList());
            // 연관관계 주인 세팅
            for (PostImage img : imgs) {
                img.setPost(saved);
            }
            imageRepo.saveAll(imgs);
            saved.getImages().addAll(imgs);
        }
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> listPosts(CategoryType category) {
        List<Post> posts;
        if (category == null || category == CategoryType.ALL) {
            posts = postRepo.findAll();
        } else if (category == CategoryType.POPULAR) {
            posts = postRepo.findPopular();
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
                        "게시글을 찾을 수 없습니다. id=" + id
                ));
        return mapToDto(post);
    }

    @Transactional
    public PostResponseDto updatePost(User owner, Long id, PostRequestDto dto) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "게시글을 찾을 수 없습니다. id=" + id
                ));
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(
                    ErrorCode.OPERATION_DENIED,
                    "수정 권한이 없습니다. id=" + id
            );
        }

        post.update(dto.getTitle(), dto.getContent(), dto.getCategory());

        // 이미지 전부 교체
        imageRepo.deleteAll(post.getImages());
        post.getImages().clear();

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> imgs = dto.getImageUrls().stream()
                    .map(PostImage::of)
                    .collect(Collectors.toList());
            for (PostImage img : imgs) {
                img.setPost(post);
            }
            imageRepo.saveAll(imgs);
            post.getImages().addAll(imgs);
        }

        return mapToDto(post);
    }

    @Transactional
    public void deletePost(User owner, Long id) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "게시글을 찾을 수 없습니다. id=" + id
                ));
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(
                    ErrorCode.OPERATION_DENIED,
                    "삭제 권한이 없습니다. id=" + id
            );
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
