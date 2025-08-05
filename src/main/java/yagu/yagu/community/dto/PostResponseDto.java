package yagu.yagu.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import yagu.yagu.community.entity.CategoryType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private CategoryType category;
    private Long ownerId;
    private String ownerNickname;
    private long likeCount;
    private long commentCount;
    private List<String> imageUrls;
}
