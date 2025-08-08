package yagu.yagu.diary.dto;

import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateGameDiaryDTO {
    // diary 입력 전용 + 기존 게임 참조
    @NotNull
    private Long gameId;

    // 프론트가 추가 입력하는 일기 정보
    private String favoriteTeam; // 사용자가 응원한 팀
    private String seat;
    private String memo;
    private String photoUrl;
}
