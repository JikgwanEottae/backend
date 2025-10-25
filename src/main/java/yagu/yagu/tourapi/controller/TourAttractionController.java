package yagu.yagu.tourapi.controller;

import yagu.yagu.tourapi.dto.TourAttractionResponseDto;
import yagu.yagu.tourapi.repository.TourAttractionRepository;
import yagu.yagu.tourapi.domain.TourAttraction;
import yagu.yagu.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attractions")
@RequiredArgsConstructor
public class TourAttractionController {

    private final TourAttractionRepository repo;

    /** 구장명으로 조회 (정렬: rank ASC), 5개 필드만 반환 */
    @GetMapping("/{stadium}")
    public ApiResponse<List<TourAttractionResponseDto>> byStadium(@PathVariable String stadium) {
        List<TourAttraction> rows = repo.findByStadiumOrderByRankingAsc(stadium);
        List<TourAttractionResponseDto> data = rows.stream().map(TourAttractionResponseDto::of).toList();
        return ApiResponse.success(data, "구장별 연관관광지 조회 성공");
    }
}