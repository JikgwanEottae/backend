package yagu.yagu.tourapi;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yagu.yagu.common.exception.BusinessException;
import yagu.yagu.common.exception.ErrorCode;
import yagu.yagu.common.response.ApiResponse;

@RestController
@RequestMapping("/api/tourism")
public class TourismController {
    private final TourismService svc;

    public TourismController(TourismService svc) {
        this.svc = svc;
    }

    /** 지역별 관광지 */
    @GetMapping(value = "/areas/{region}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> areaListRaw(
            @PathVariable Region region,
            @RequestParam(defaultValue = "12") int contentTypeId,
            @RequestParam(defaultValue = "10") int numOfRows,
            @RequestParam(defaultValue = "1") int pageNo) {

        // 파라미터 유효성 검사
        if (numOfRows <= 0 || numOfRows > 1000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "numOfRows는 1~1000 사이여야 합니다.");
        }
        if (pageNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "pageNo는 1 이상이어야 합니다.");
        }
        if (contentTypeId < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "contentTypeId는 0 이상이어야 합니다.");
        }

        try {
            String json = svc.getAreaListJson(region, contentTypeId, numOfRows, pageNo);
            return ResponseEntity.ok(ApiResponse.success(json, "지역별 관광지 조회 완료"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOURISM_API_ERROR,
                    "관광 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /** 구단별 연관관광지 */
    @GetMapping(value = "/stadiums/{stadium}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<String>> relatedStadiumRaw(
            @PathVariable Stadium stadium,
            @RequestParam(defaultValue = "10") int numOfRows,
            @RequestParam(defaultValue = "1") int pageNo) {

        // 파라미터 유효성 검사
        if (numOfRows <= 0 || numOfRows > 1000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "numOfRows는 1~1000 사이여야 합니다.");
        }
        if (pageNo <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "pageNo는 1 이상이어야 합니다.");
        }

        try {
            String json = svc.getRelatedListJson(stadium, numOfRows, pageNo);
            return ResponseEntity.ok(ApiResponse.success(json, "구단별 관광지 조회 완료"));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOURISM_API_ERROR,
                    "구단별 관광 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
