package yagu.yagu.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;


    public static PostImage of(String imageUrl) {
        PostImage img = new PostImage();
        img.imageUrl = imageUrl;
        return img;
    }

    public void assignToPost(Post post) {
        this.post = post;
        post.getImages().add(this);
    }
}
