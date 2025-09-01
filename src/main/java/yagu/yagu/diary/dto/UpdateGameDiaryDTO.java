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
    private Boolean isRemoveImage;
}
