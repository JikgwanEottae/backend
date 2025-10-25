package yagu.yagu.tourapi.controller;

import yagu.yagu.tourapi.dto.TourAttractionResponseDto;
import yagu.yagu.tourapi.dto.TourAttractionListResponseDto;
import yagu.yagu.tourapi.repository.TourAttractionRepository;
import yagu.yagu.tourapi.domain.TourAttraction;
import yagu.yagu.tourapi.domain.TeamStadiumMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attractions")
@RequiredArgsConstructor
public class TourAttractionController {

    private final TourAttractionRepository repo;

    /** 팀명으로 연관 관광지 조회 */
    @GetMapping("/{team}")
    public TourAttractionListResponseDto getAttractionsByTeam(@PathVariable String team) {
        try {
            // 팀명 -> 구장명 변환
            String stadium = TeamStadiumMapper.getStadiumByTeam(team);

            // 구장명으로 관광지 조회
            List<TourAttraction> rows = repo.findByStadiumOrderByRankingAsc(stadium);
            List<TourAttractionResponseDto> data = rows.stream()
                    .map(TourAttractionResponseDto::of)
                    .toList();

            // 성공 응답
            return TourAttractionListResponseDto.builder()
                    .result(true)
                    .httpCode(200)
                    .stadium(stadium)
                    .data(data)
                    .message("구장별 연관관광지 조회 성공")
                    .build();
        } catch (IllegalArgumentException e) {
            // 잘못된 팀명 에러 응답
            return TourAttractionListResponseDto.builder()
                    .result(false)
                    .httpCode(400)
                    .stadium(null)
                    .data(null)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            // 서버 에러 응답
            return TourAttractionListResponseDto.builder()
                    .result(false)
                    .httpCode(500)
                    .stadium(null)
                    .data(null)
                    .message("서버 오류가 발생했습니다")
                    .build();
        }
    }
}