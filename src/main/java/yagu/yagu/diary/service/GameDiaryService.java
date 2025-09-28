package yagu.yagu.diary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yagu.yagu.diary.dto.CreateGameDiaryDTO;
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.dto.UserStatsDTO;
import yagu.yagu.diary.dto.UpdateGameDiaryDTO;
import yagu.yagu.diary.entity.GameDiary;
import yagu.yagu.diary.entity.UserStats;
import yagu.yagu.diary.repository.GameDiaryRepository;
import yagu.yagu.diary.repository.UserStatsRepository;
import yagu.yagu.game.entity.KboGame;
import yagu.yagu.game.repository.KboGameRepository;
import yagu.yagu.user.entity.User;
import yagu.yagu.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameDiaryService {
        private final UserRepository userRepo;
        private final GameDiaryRepository diaryRepo;
        private final UserStatsRepository statsRepo;
        private final KboGameRepository gameRepo;

        public GameDiaryDetailDTO getDiaryDetail(Long userId, Long diaryId) {
                GameDiary d = diaryRepo.findById(diaryId)
                                .filter(g -> g.getUser().getId().equals(userId))
                                .orElseThrow(() -> new RuntimeException("Diary not found"));

                return toDto(d);
        }

        @Transactional(readOnly = true)
        public List<GameDiaryDetailDTO> getAllDiaries(Long userId) {
                return diaryRepo.findAllDtosByUser(userId);
        }

        @Transactional
        public Long createDiary(Long userId, CreateGameDiaryDTO dto) {
                User user = userRepo.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                KboGame game = gameRepo.findById(dto.getGameId())
                                .orElseThrow(() -> new RuntimeException("Game not found"));

                GameDiary.Result result = mapToResult(game.getWinTeam(), dto.getFavoriteTeam());

                // 엔티티 생성
                GameDiary diary = GameDiary.of(
                                user,
                                game,
                                dto.getFavoriteTeam(),
                                dto.getTitle(),
                                result,
                                dto.getSeat(),
                                dto.getMemo(),
                                dto.getPhotoUrl());
                diaryRepo.save(diary);

                // 통계 생성/업데이트
                UserStats stats = statsRepo.findById(userId)
                                .orElseGet(() -> {
                                        UserStats newStats = new UserStats(user);
                                        return statsRepo.save(newStats);
                                });
                if (result != null) {
                        stats.updateOnNew(toStatsResult(result));
                }
                statsRepo.save(stats);

                return diary.getId();
        }

        @Transactional
        public void updateDiary(Long userId, Long diaryId, UpdateGameDiaryDTO dto) {
                GameDiary diary = diaryRepo.findById(diaryId)
                                .filter(d -> d.getUser().getId().equals(userId))
                                .orElseThrow(() -> new RuntimeException("Diary not found"));

                // 이전/새 결과 계산
                GameDiary.Result oldRes = diary.getResult();
                KboGame game = diary.getGame(); // 수정 시 gameId 변경 없음
                GameDiary.Result newRes = mapToResult(game.getWinTeam(), dto.getFavoriteTeam());

                // 사진 URL 결정: 파일이 있으면 새 URL로 교체
                // - dto.photoUrl가 빈 문자열("")이면 삭제(null 저장)로 간주
                // - 그 외에는 기존 URL 유지
                String resolvedPhotoUrl;
                if (dto.getPhotoUrl() != null) {
                        resolvedPhotoUrl = dto.getPhotoUrl().isBlank() ? null : dto.getPhotoUrl();
                } else {
                        resolvedPhotoUrl = diary.getPhotoUrl();
                }

                // 엔티티 업데이트
                diary.update(
                                null, // game 변경 없음
                                dto.getFavoriteTeam(),
                                dto.getTitle(),
                                newRes,
                                dto.getSeat(),
                                dto.getMemo(),
                                resolvedPhotoUrl);

                // 엔티티 update는 photoUrl이 null이면 값을 변경하지 않으므로,
                // 삭제 의도(빈 문자열 전달)일 때는 명시적으로 null로 설정
                if (dto.getPhotoUrl() != null && dto.getPhotoUrl().isBlank()) {
                        diary.setPhotoUrl(null);
                }

                // 통계 업데이트
                User user = diary.getUser();
                UserStats stats = statsRepo.findById(userId)
                                .orElseGet(() -> {
                                        UserStats newStats = new UserStats(user);
                                        return statsRepo.save(newStats);
                                });
                if (oldRes != null || newRes != null) {
                        // null은 통계 반영 제외: old/null -> new/null 케이스 분기
                        if (oldRes == null && newRes != null) {
                                stats.updateOnNew(toStatsResult(newRes));
                        } else if (oldRes != null && newRes == null) {
                                stats.updateOnDelete(toStatsResult(oldRes));
                        } else if (oldRes != null) { // 둘 다 null 아님
                                stats.updateOnChange(toStatsResult(oldRes), toStatsResult(newRes));
                        }
                }
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
                User user = diary.getUser();
                UserStats stats = statsRepo.findById(userId)
                                .orElseGet(() -> {
                                        UserStats newStats = new UserStats(user);
                                        return statsRepo.save(newStats);
                                });
                if (oldRes != null) {
                        stats.updateOnDelete(toStatsResult(oldRes));
                }
                statsRepo.save(stats);
        }

        @Transactional(readOnly = true)
        public List<GameDiaryDetailDTO> getDiariesByMonth(Long userId, int year, int month) {
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate endExclusive = start.plusMonths(1);
                return diaryRepo.findMonthDtos(userId, start, endExclusive);
        }

        @Transactional
        public UserStatsDTO getUserStats(Long userId) {
                User user = userRepo.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                UserStats stats = statsRepo.findById(userId)
                                .orElseGet(() -> {
                                        UserStats newStats = new UserStats(user);
                                        return statsRepo.save(newStats);
                                });

                // UserStats를 UserStatsDTO로 변환
                return UserStatsDTO.builder()
                                .userId(stats.getUserId())
                                .winCount(stats.getWinCount())
                                .lossCount(stats.getLossCount())
                                .drawCount(stats.getDrawCount())
                                .winRate(stats.getWinRate())
                                .build();
        }

        private GameDiaryDetailDTO toDto(GameDiary d) {
                return GameDiaryDetailDTO.builder()
                                .diaryId(d.getId())
                                .date(d.getGame().getGameDate())
                                .gameTime(d.getGame().getGameTime())
                                .homeScore(d.getGame().getHomeScore() == null ? 0 : d.getGame().getHomeScore())
                                .awayScore(d.getGame().getAwayScore() == null ? 0 : d.getGame().getAwayScore())
                                .homeTeam(d.getGame().getHomeTeam())
                                .awayTeam(d.getGame().getAwayTeam())
                                .winTeam(d.getGame().getWinTeam())
                                .favoriteTeam(d.getFavoriteTeam())
                                .title(d.getTitle())
                                .result(d.getResult() == null ? null : d.getResult().name())
                                .stadium(d.getGame().getStadium())
                                .seat(d.getSeat())
                                .memo(d.getMemo())
                                .photoUrl(d.getPhotoUrl())
                                .build();
        }

        private GameDiary.Result mapToResult(String winTeam, String supportTeam) {
                if (winTeam == null) {
                        return null; // 경기 결과 미정: null 유지
                }
                // 응원팀이 비어 있으면 결과도 null 처리
                if (supportTeam == null || supportTeam.isBlank()) {
                        return null;
                }
                if ("무승부".equalsIgnoreCase(winTeam) || "DRAW".equalsIgnoreCase(winTeam)) {
                        return GameDiary.Result.DRAW;
                }
                return winTeam.equalsIgnoreCase(supportTeam)
                                ? GameDiary.Result.WIN
                                : GameDiary.Result.LOSS;
        }

        private UserStats.Result toStatsResult(GameDiary.Result r) {
                if (r == null)
                        return null;
                return switch (r) {
                        case WIN -> UserStats.Result.WIN;
                        case LOSS -> UserStats.Result.LOSS;
                        case DRAW -> UserStats.Result.DRAW;
                };
        }

        @Transactional
        public void patchDiary(Long userId, Long diaryId, Map<String, Object> updates) {
                GameDiary diary = diaryRepo.findById(diaryId)
                                .filter(d -> d.getUser().getId().equals(userId))
                                .orElseThrow(() -> new RuntimeException("Diary not found"));

                // 허용된 필드만 반영
                Set<String> allowed = Set.of("favoriteTeam", "title", "seat", "memo", "photoUrl");
                updates.keySet().removeIf(k -> !allowed.contains(k));

                GameDiary.Result oldRes = diary.getResult();
                KboGame game = diary.getGame();

                boolean hasFavoriteTeam = updates.containsKey("favoriteTeam");
                boolean hasTitle = updates.containsKey("title");
                boolean hasSeat = updates.containsKey("seat");
                boolean hasMemo = updates.containsKey("memo");
                boolean hasPhotoUrl = updates.containsKey("photoUrl");

                String favoriteTeam = hasFavoriteTeam ? toNullableString(updates.get("favoriteTeam"))
                                : diary.getFavoriteTeam();
                String title = hasTitle ? toNullableString(updates.get("title")) : diary.getTitle();
                String seat = hasSeat ? toNullableString(updates.get("seat")) : diary.getSeat();
                String memo = hasMemo ? toNullableString(updates.get("memo")) : diary.getMemo();

                GameDiary.Result newRes = mapToResult(game.getWinTeam(), favoriteTeam);

                String photoUrlArg = null;
                boolean deletePhotoAfterUpdate = false;
                if (hasPhotoUrl) {
                        String provided = toNullableString(updates.get("photoUrl"));
                        if (provided == null || provided.isBlank()) {
                                photoUrlArg = ""; // 임시로 빈 문자열 세팅 후 아래에서 null로 처리
                                deletePhotoAfterUpdate = true;
                        } else {
                                photoUrlArg = provided;
                        }
                }

                diary.update(
                                null, // game은 PATCH에서 변경하지 않음
                                favoriteTeam,
                                title,
                                newRes,
                                seat,
                                memo,
                                photoUrlArg);

                if (deletePhotoAfterUpdate) {
                        diary.setPhotoUrl(null);
                }

                // 통계 업데이트
                User user = diary.getUser();
                UserStats stats = statsRepo.findById(userId)
                                .orElseGet(() -> {
                                        UserStats newStats = new UserStats(user);
                                        return statsRepo.save(newStats);
                                });
                if (oldRes != null || newRes != null) {
                        if (oldRes == null && newRes != null) {
                                stats.updateOnNew(toStatsResult(newRes));
                        } else if (oldRes != null && newRes == null) {
                                stats.updateOnDelete(toStatsResult(oldRes));
                        } else if (oldRes != null) {
                                stats.updateOnChange(toStatsResult(oldRes), toStatsResult(newRes));
                        }
                }
                statsRepo.save(stats);
        }

        private String toNullableString(Object value) {
                return value == null ? null : value.toString();
        }
}