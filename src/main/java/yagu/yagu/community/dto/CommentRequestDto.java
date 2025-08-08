package yagu.yagu.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {
    @NotBlank(message = "댓글 내용을 입력하세요.")
    @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
    private String content;
    private Long parentCommentId;
}
