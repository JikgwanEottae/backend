package yagu.yagu.tourapi;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tourism")
public class TourismController {
    private final TourismService svc;

    public TourismController(TourismService svc) {
        this.svc = svc;
    }

    /**
     * 지역별 관광지
     */
    @GetMapping(value = "/areas/{region}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> areaListRaw(
            @PathVariable Region region,
            @RequestParam(defaultValue = "12") int contentTypeId,
            @RequestParam(defaultValue = "10") int numOfRows,
            @RequestParam(defaultValue = "1") int pageNo) {
        String json = svc.getAreaListJson(region, contentTypeId, numOfRows, pageNo);
        return ResponseEntity.ok(json);
    }

    /**
     * 구단별 연관관광지
     */
    @GetMapping(value = "/stadiums/{stadium}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> relatedStadiumRaw(
            @PathVariable Stadium stadium,
            @RequestParam(defaultValue = "10") int numOfRows,
            @RequestParam(defaultValue = "1") int pageNo) {
        String json = svc.getRelatedListJson(stadium, numOfRows, pageNo);
        return ResponseEntity.ok(json);
    }
}
