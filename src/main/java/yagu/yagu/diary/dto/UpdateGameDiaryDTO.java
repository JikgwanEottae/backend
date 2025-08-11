package yagu.yagu.diary.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateGameDiaryDTO {
    private String favoriteTeam;
    private String seat;
    private String memo;
    private String photoUrl;
    // true로 보내면 기존 이미지를 제거(null 저장)
    private Boolean removePhoto;
}
