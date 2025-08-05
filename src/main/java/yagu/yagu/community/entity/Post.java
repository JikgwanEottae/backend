package yagu.yagu.community.entity;

import jakarta.persistence.*;
import lombok.*;
import yagu.yagu.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PostImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PostLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Comment> comments = new ArrayList<>();

    // 생성 전용 팩토리 (엔티티에선 빌더 금지)
    public static Post create(String title, String content, CategoryType category, User owner) {
        Post p = new Post();
        p.title = title;
        p.content = content;
        p.category = category;
        p.owner = owner;
        return p;
    }

    // 도메인 동작
    public void update(String title, String content, CategoryType category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public void replaceImages(List<PostImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            for (PostImage img : newImages) {
                img.setPost(this);
                this.images.add(img);
            }
        }
    }
}