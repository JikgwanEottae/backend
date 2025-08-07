package yagu.yagu.diary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.diary.dto.CreateGameDiaryDTO;
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.entity.GameDiary;
import yagu.yagu.diary.entity.UserStats;
import yagu.yagu.diary.repository.GameDiaryRepository;
import yagu.yagu.diary.repository.UserStatsRepository;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameDiaryService {
    private final UserRepository      userRepo;
    private final GameDiaryRepository diaryRepo;
    private final UserStatsRepository statsRepo;

    public GameDiaryDetailDTO getDiaryDetail(Long userId, Long diaryId) {
        GameDiary d = diaryRepo.findById(diaryId)
                .filter(g -> g.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));

        return toDto(d);
    }

    public List<GameDiaryDetailDTO> getAllDiaries(Long userId) {
        return diaryRepo.findAllByUserIdOrderByGameDateDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Long createDiary(Long userId, CreateGameDiaryDTO dto) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GameDiary.Result result = mapToResult(dto.getWinTeam(), dto.getFavoriteTeam());

        // 엔티티 생성
        GameDiary diary = GameDiary.of(
                user,
                dto.getGameDate(),
                dto.getGameTime(),
                dto.getHomeTeam(),
                dto.getAwayTeam(),
                dto.getHomeScore(),
                dto.getAwayScore(),
                dto.getWinTeam(),
                dto.getFavoriteTeam(),
                result,
                dto.getStadium(),
                dto.getSeat(),
                dto.getMemo(),
                dto.getPhotoUrl()
        );
        diaryRepo.save(diary);

        // 통계 생성/업데이트
        UserStats stats = statsRepo.findById(userId)
                .orElse(new UserStats(userId));
        stats.updateOnNew(toStatsResult(result));
        statsRepo.save(stats);

        return diary.getId();
    }

    @Transactional
    public void updateDiary(Long userId, Long diaryId, CreateGameDiaryDTO dto) {
        GameDiary diary = diaryRepo.findById(diaryId)
                .filter(d -> d.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));

        // 이전/새 결과 계산
        GameDiary.Result oldRes = diary.getResult();
        GameDiary.Result newRes = mapToResult(dto.getWinTeam(), dto.getFavoriteTeam());

        // 엔티티 업데이트
        diary.update(
                dto.getGameDate(),
                dto.getGameTime(),
                dto.getHomeTeam(),
                dto.getAwayTeam(),
                dto.getHomeScore(),
                dto.getAwayScore(),
                dto.getWinTeam(),
                dto.getFavoriteTeam(),
                newRes,
                dto.getStadium(),
                dto.getSeat(),
                dto.getMemo(),
                dto.getPhotoUrl()
        );

        // 통계 업데이트
        UserStats stats = statsRepo.findById(userId)
                .orElse(new UserStats(userId));
        stats.updateOnChange(toStatsResult(oldRes), toStatsResult(newRes));
        statsRepo.save(stats);
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        GameDiary diary = diaryRepo.findById(diaryId)
                .filter(d -> d.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));

        // 삭제 전 결과 보관
        GameDiary.Result oldRes = diary.getResult();
        diaryRepo.delete(diary);

        // 통계 업데이트
        UserStats stats = statsRepo.findById(userId)
                .orElse(new UserStats(userId));
        stats.updateOnDelete(toStatsResult(oldRes));
        statsRepo.save(stats);
    }

    public List<GameDiaryDetailDTO> getDiariesByMonth(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());

        return diaryRepo.findAllByUserIdAndGameDateBetweenOrderByGameDateDesc(userId, start, end)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UserStats getUserStats(Long userId) {
        return statsRepo.findById(userId)
                .orElse(new UserStats(userId));
    }

    private GameDiaryDetailDTO toDto(GameDiary d) {
        return GameDiaryDetailDTO.builder()
                .diaryId(d.getId())
                .date(d.getGameDate())
                .gameTime(d.getGameTime())
                .homeScore(d.getHomeScore())
                .awayScore(d.getAwayScore())
                .homeTeam(d.getHomeTeam())
                .awayTeam(d.getAwayTeam())
                .winTeam(d.getWinTeam())
                .favoriteTeam(d.getFavoriteTeam())
                .result(d.getResult().name())
                .stadium(d.getStadium())
                .seat(d.getSeat())
                .memo(d.getMemo())
                .photoUrl(d.getPhotoUrl())
                .build();
    }

    private GameDiary.Result mapToResult(String winTeam, String supportTeam) {
        if ("무승부".equalsIgnoreCase(winTeam)) {
            return GameDiary.Result.DRAW;
        }
        return winTeam.equalsIgnoreCase(supportTeam)
                ? GameDiary.Result.WIN
                : GameDiary.Result.LOSS;
    }

    private UserStats.Result toStatsResult(GameDiary.Result r) {
        return switch (r) {
            case WIN  -> UserStats.Result.WIN;
            case LOSS -> UserStats.Result.LOSS;
            case DRAW -> UserStats.Result.DRAW;
        };
    }
}