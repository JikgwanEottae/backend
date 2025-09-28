package yagu.yagu.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import yagu.yagu.diary.dto.GameDiaryDetailDTO;
import yagu.yagu.diary.entity.GameDiary;

import java.time.LocalDate;
import java.util.List;

public interface GameDiaryRepository extends JpaRepository<GameDiary, Long> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("delete from GameDiary gd where gd.user.id = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  @Query("""
        select new yagu.yagu.diary.dto.GameDiaryDetailDTO(
          d.id,
          g.gameDate,
          g.gameTime,
          coalesce(g.homeScore, 0),
          coalesce(g.awayScore, 0),
          g.winTeam,
          d.favoriteTeam,
          d.title,
          g.homeTeam,
          g.awayTeam,
          cast(d.result as string),
          g.stadium,
          d.seat,
          d.memo,
          d.photoUrl
        )
        from GameDiary d
        join d.game g
        where d.user.id = :userId
        order by g.gameDate desc
      """)
  List<GameDiaryDetailDTO> findAllDtosByUser(@Param("userId") Long userId);

  @Query("""
        select new yagu.yagu.diary.dto.GameDiaryDetailDTO(
          d.id,
          g.gameDate,
          g.gameTime,
          coalesce(g.homeScore, 0),
          coalesce(g.awayScore, 0),
          g.winTeam,
          d.favoriteTeam,
          d.title,
          g.homeTeam,
          g.awayTeam,
          cast(d.result as string),
          g.stadium,
          d.seat,
          d.memo,
          d.photoUrl
        )
        from GameDiary d
        join d.game g
        where d.user.id = :userId
          and g.gameDate >= :start
          and g.gameDate < :endExclusive
        order by g.gameDate desc
      """)
  List<GameDiaryDetailDTO> findMonthDtos(
      @Param("userId") Long userId,
      @Param("start") LocalDate start,
      @Param("endExclusive") LocalDate endExclusive);
}