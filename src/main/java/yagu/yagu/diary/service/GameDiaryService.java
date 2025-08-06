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
    private final UserRepository userRepo;
    private final GameDiaryRepository diaryRepo;
    private final UserStatsRepository statsRepo;

    public GameDiaryDetailDTO getDiaryDetail(Long userId, Long diaryId) {
        GameDiary d = diaryRepo.findById(diaryId)
                .filter(g -> g.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));
        return GameDiaryDetailDTO.builder()
                .diaryId(d.getId())
                .date(d.getGameDate())
                .gameTime(d.getGameTime())
                .homeScore(d.getHomeScore())
                .awayScore(d.getAwayScore())
                .winTeam(d.getWinTeam())
                .favoriteTeam(d.getFavoriteTeam())
                .homeTeam(d.getHomeTeam())
                .awayTeam(d.getAwayTeam())
                .result(d.getResult().name())
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
                        .gameTime(d.getGameTime())
                        .homeScore(d.getHomeScore())
                        .awayScore(d.getAwayScore())
                        .winTeam(d.getWinTeam())
                        .favoriteTeam(d.getFavoriteTeam())
                        .homeTeam(d.getHomeTeam())
                        .awayTeam(d.getAwayTeam())
                        .result(d.getResult().name())
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

        GameDiary.Result result = mapToResult(dto.getWinTeam(), dto.getFavoriteTeam());

        GameDiary diary = GameDiary.builder()
                .user(user)
                .gameDate(dto.getGameDate())
                .gameTime(dto.getGameTime())
                .homeTeam(dto.getHomeTeam())
                .awayTeam(dto.getAwayTeam())
                .homeScore(dto.getHomeScore())
                .awayScore(dto.getAwayScore())
                .winTeam(dto.getWinTeam())
                .favoriteTeam(dto.getFavoriteTeam())
                .result(result)
                .stadium(dto.getStadium())
                .seat(dto.getSeat())
                .memo(dto.getMemo())
                .photoUrl(dto.getPhotoUrl())
                .build();

        diaryRepo.save(diary);

        UserStats stats = statsRepo.findById(userId)
                .orElse(UserStats.builder().userId(userId).build());
        stats.updateOnNew(toStatsResult(result));
        statsRepo.save(stats);

        return diary.getId();
    }

    @Transactional
    public void updateDiary(Long userId, Long diaryId, CreateGameDiaryDTO dto) {
        GameDiary diary = diaryRepo.findById(diaryId)
                .filter(d -> d.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Diary not found"));

        // 1) 이전 결과 저장
        GameDiary.Result oldResult = diary.getResult();
        // 2) 새 결과 계산
        GameDiary.Result newResult = mapToResult(dto.getWinTeam(), dto.getFavoriteTeam());

        // 3) DTO의 모든 필드를 엔티티에 반영
        diary.setGameDate(dto.getGameDate());
        diary.setGameTime(dto.getGameTime());
        diary.setHomeTeam(dto.getHomeTeam());
        diary.setAwayTeam(dto.getAwayTeam());
        diary.setHomeScore(dto.getHomeScore());
        diary.setAwayScore(dto.getAwayScore());
        diary.setWinTeam(dto.getWinTeam());
        diary.setFavoriteTeam(dto.getFavoriteTeam());
        diary.setResult(newResult);
        diary.setStadium(dto.getStadium());
        diary.setSeat(dto.getSeat());
        diary.setMemo(dto.getMemo());
        // photoUrl은 null 체크 후에만 교체
        if (dto.getPhotoUrl() != null) {
            diary.setPhotoUrl(dto.getPhotoUrl());
        }

        diaryRepo.save(diary);

        // 4) 통계 업데이트
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

    /** 년·월로 해당 달의 일기 리스트 조회 */
    public List<GameDiaryDetailDTO> getDiariesByMonth(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return diaryRepo
                .findAllByUserIdAndGameDateBetweenOrderByGameDateDesc(userId, start, end)
                .stream()
                .map(d -> GameDiaryDetailDTO.builder()
                        .diaryId(d.getId())
                        .date(d.getGameDate())
                        .gameTime(d.getGameTime())
                        .homeScore(d.getHomeScore())
                        .awayScore(d.getAwayScore())
                        .winTeam(d.getWinTeam())
                        .favoriteTeam(d.getFavoriteTeam())
                        .homeTeam(d.getHomeTeam())
                        .awayTeam(d.getAwayTeam())
                        .result(d.getResult().name())
                        .stadium(d.getStadium())
                        .seat(d.getSeat())
                        .memo(d.getMemo())
                        .photoUrl(d.getPhotoUrl())
                        .build()
                )
                .collect(Collectors.toList());
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