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

        //  winTeam / supportTeam 비교로 Result 계산
        GameDiary.Result result = mapToResult(dto.getWinTeam(), dto.getFavoriteTeam());

        GameDiary diary = GameDiary.builder()
                .user(user)
                .gameDate(dto.getGameDate())
                .homeTeam(dto.getHomeTeam())
                .awayTeam(dto.getAwayTeam())
                .result(result)
                .score(dto.getHomeScore() + "-" + dto.getAwayScore())
                .stadium(dto.getStadium())
                .seat(dto.getSeat())
                .memo(dto.getMemo())
                .photoUrl(dto.getPhotoUrl())
                .build();
        diaryRepo.save(diary);

        // 통계 업데이트
        UserStats stats = statsRepo.findById(userId)
                .orElse(UserStats.builder().userId(userId).build());
        stats.updateOnNew(toStatsResult(result));
        statsRepo.save(stats);

        return diary.getId();
    }

    @Transactional
    public void updateDiary(Long userId, Long diaryId, CreateGameDiaryDTO dto) {
        //  기존 다이어리 조회
        GameDiary diary = diaryRepo.findById(diaryId)
                .filter(d -> d.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));

        //  이전 결과 저장
        GameDiary.Result oldResult = diary.getResult();

        //  새 result 계산
        GameDiary.Result newResult = mapToResult(dto.getWinTeam(), dto.getFavoriteTeam());

        //  엔티티 필드 업데이트
        diary.setResult(newResult);
        diary.setScore(dto.getHomeScore() + "-" + dto.getAwayScore());
        diary.setStadium(dto.getStadium());
        diary.setSeat(dto.getSeat());
        diary.setMemo(dto.getMemo());
        diary.setPhotoUrl(dto.getPhotoUrl());

        diaryRepo.save(diary);

        //  통계 갱신 (old → new)
        UserStats stats = statsRepo.findById(userId)
                .orElse(UserStats.builder().userId(userId).build());
        stats.updateOnChange(toStatsResult(oldResult), toStatsResult(newResult));
        statsRepo.save(stats);
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        // 권한 체크 포함하여 기존 다이어리 조회
        GameDiary diary = diaryRepo.findById(diaryId)
                .filter(d -> d.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));

        // 삭제 전 결과 저장
        GameDiary.Result oldResult = diary.getResult();

        // 삭제
        diaryRepo.delete(diary);

        // 통계 반영
        UserStats stats = statsRepo.findById(userId)
                .orElse(UserStats.builder().userId(userId).build());
        stats.updateOnDelete(toStatsResult(oldResult));
        statsRepo.save(stats);
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

    /** "승리팀" == favoriteTeam → WIN, "무승부" → DRAW, 나머지 → LOSS */
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