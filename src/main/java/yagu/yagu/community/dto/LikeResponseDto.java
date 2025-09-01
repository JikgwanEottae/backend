package yagu.yagu.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponseDto {
    private Long likeCount;
    private boolean likedByCurrentUser;
}
