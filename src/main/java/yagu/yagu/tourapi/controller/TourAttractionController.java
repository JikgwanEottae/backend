package yagu.yagu.tourapi.controller;

import yagu.yagu.tourapi.dto.TourAttractionResponseDto;
import yagu.yagu.tourapi.dto.TourAttractionListResponseDto;
import yagu.yagu.tourapi.repository.TourAttractionRepository;
import yagu.yagu.tourapi.domain.TourAttraction;
import yagu.yagu.tourapi.domain.TeamStadiumMapper;
import yagu.yagu.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attractions")
@RequiredArgsConstructor
public class TourAttractionController {

    private final TourAttractionRepository repo;

    /** 팀명으로 연관 관광지 조회  */
    @GetMapping("/{team}")
    public ApiResponse<TourAttractionListResponseDto> getAttractionsByTeam(@PathVariable String team) {
        // 팀명 -> 구장명 변환
        String stadium = TeamStadiumMapper.getStadiumByTeam(team);

        // 구장명으로 관광지 조회
        List<TourAttraction> rows = repo.findByStadiumOrderByRankingAsc(stadium);
        List<TourAttractionResponseDto> attractions = rows.stream()
                .map(TourAttractionResponseDto::of)
                .toList();

        // 응답 데이터 구성
        TourAttractionListResponseDto responseData = TourAttractionListResponseDto.builder()
                .stadium(stadium)
                .attractions(attractions)
                .build();

        return ApiResponse.success(responseData, "구장별 연관관광지 조회 성공");
    }
}