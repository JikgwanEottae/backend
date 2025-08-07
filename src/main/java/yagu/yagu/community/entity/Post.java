package yagu.yagu.community.entity;

import jakarta.persistence.*;
import lombok.*;
import yagu.yagu.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
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

    /** 정적 팩토리로만 생성 */
    public static Post create(String title, String content, CategoryType category, User owner) {
        Post p = new Post();
        p.title = title;
        p.content = content;
        p.category = category;
        p.owner = owner;
        return p;
    }

    public void update(String title, String content, CategoryType category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }


    public void replaceImages(List<PostImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            for (PostImage img : newImages) {
                img.assignToPost(this);
            }
        }
    }
}