package yagu.yagu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .category(dto.getCategory())
                .owner(owner)
                .build();
        Post saved = postRepo.save(post);

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> imgs = dto.getImageUrls().stream()
                    .map(url -> PostImage.builder().imageUrl(url).post(saved).build())
                    .collect(Collectors.toList());
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
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        return mapToDto(post);
    }

    @Transactional
    public PostResponseDto updatePost(User owner, Long id, PostRequestDto dto) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        post.setCategory(dto.getCategory());

        imageRepo.deleteAll(post.getImages());
        post.getImages().clear();
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> imgs = dto.getImageUrls().stream()
                    .map(url -> PostImage.builder().imageUrl(url).post(post).build())
                    .collect(Collectors.toList());
            imageRepo.saveAll(imgs);
            post.getImages().addAll(imgs);
        }
        return mapToDto(post);
    }

    @Transactional
    public void deletePost(User owner, Long id) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + id));
        if (!post.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        postRepo.delete(post);
    }

    private PostResponseDto mapToDto(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .ownerId(post.getOwner().getId())
                .ownerNickname(post.getOwner().getNickname())
                .likeCount(post.getLikes().size())
                .commentCount(post.getComments().size())
                .imageUrls(post.getImages().stream()
                        .map(PostImage::getImageUrl)
                        .collect(Collectors.toList()))
                .build();
    }
}
