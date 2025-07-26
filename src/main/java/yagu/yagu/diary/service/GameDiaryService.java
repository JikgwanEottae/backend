package yagu.yagu.diary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.diary.dto.CreateGameDiaryDTO;
import yagu.yagu.diary.dto.GameDiaryCalendarDTO;
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.entity.GameDiary;
import yagu.yagu.diary.entity.UserStats;
import yagu.yagu.diary.repository.GameDiaryRepository;
import yagu.yagu.diary.repository.UserStatsRepository;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameDiaryService {
    private final UserRepository userRepo;
    private final GameDiaryRepository diaryRepo;
    private final UserStatsRepository statsRepo;

    public List<GameDiaryCalendarDTO> getMonthlyDiaries(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return diaryRepo.findAllByUserIdAndGameDateBetween(userId, start, end).stream()
                .map(d -> GameDiaryCalendarDTO.builder()
                        .diaryId(d.getId())
                        .date(d.getGameDate())
                        .result(d.getResult().name())
                        .score(d.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    public GameDiaryDetailDTO getDiaryDetail(Long userId, Long diaryId) {
        GameDiary d = diaryRepo.findById(diaryId)
                .filter(g -> g.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));
        return GameDiaryDetailDTO.builder()
                .diaryId(d.getId())
                .date(d.getGameDate())
                .homeTeam(d.getHomeTeam())
                .awayTeam(d.getAwayTeam())
                .result(d.getResult().name())
                .score(d.getScore())
                .stadium(d.getStadium())
                .seat(d.getSeat())
                .memo(d.getMemo())
                .photoUrl(d.getPhotoUrl())
                .build();
    }

    public List<GameDiaryDetailDTO> getAllDiaries(Long userId) {
        return diaryRepo.findAllByUserIdOrderByGameDateDesc(userId)
                .stream()
                .map(d -> GameDiaryDetailDTO.builder()
                        .diaryId(d.getId())
                        .date(d.getGameDate())
                        .homeTeam(d.getHomeTeam())
                        .awayTeam(d.getAwayTeam())
                        .result(d.getResult().name())
                        .score(d.getScore())
                        .stadium(d.getStadium())
                        .seat(d.getSeat())
                        .memo(d.getMemo())
                        .photoUrl(d.getPhotoUrl())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createDiary(Long userId, CreateGameDiaryDTO dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GameDiary diary = GameDiary.builder()
                .user(user)
                .gameDate(dto.getDate())
                .homeTeam(dto.getHomeTeam())
                .awayTeam(dto.getAwayTeam())
                .result(GameDiary.Result.valueOf(dto.getResult()))
                .score(dto.getScore())
                .stadium(dto.getStadium())
                .seat(dto.getSeat())
                .memo(dto.getMemo())
                .photoUrl(dto.getPhotoUrl())
                .build();
        diary = diaryRepo.save(diary);

        // stats 업데이트
        UserStats stats = statsRepo.findById(userId)
                .orElse(UserStats.builder().userId(userId).build());
        stats.updateOnNew(UserStats.Result.valueOf(dto.getResult()));
        statsRepo.save(stats);

        return diary.getId();
    }

    //승률 조회
    public UserStats getUserStats(Long userId) {
        return statsRepo.findById(userId)
                .orElse(UserStats.builder()
                        .userId(userId)
                        .winCount(0)
                        .lossCount(0)
                        .drawCount(0)
                        .winRate(0.0)
                        .build());
    }

}