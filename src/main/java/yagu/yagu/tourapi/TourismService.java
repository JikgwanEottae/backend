package yagu.yagu.tourapi;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TourismService {
    private final TourismProperties props;
    private final RestTemplate rt;

    public TourismService(TourismProperties props, RestTemplate rt) {
        this.props = props;
        this.rt = rt;
    }

    private String encodeKey(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    /** 지역별 관광지 조회 */
    public String getAreaListJson(Region r, int ct, int rows, int page) {
        String key = encodeKey(props.getArea().getServiceKey());
        String url = String.format(
                "%s/areaBasedList2?serviceKey=%s&MobileOS=IOS&MobileApp=yaguApp" +
                        "&arrange=C&contentTypeId=%d&areaCode=%d&numOfRows=%d&pageNo=%d&_type=json",
                props.getArea().getBaseUrl(), key, ct, r.getAreaCode(), rows, page);
        return rt.getForObject(URI.create(url), String.class);
    }

    /** 구단별 연관관광지 조회 */
    public String getRelatedListJson(Stadium s, int rows, int page) {
        String key = encodeKey(props.getRelate().getServiceKey());
        String baseYm = "202506"; // openapi 업데이트가 실시간으로 되지 않아서 하드코딩
        String url = String.format(
                "%s/areaBasedList1?serviceKey=%s&MobileOS=IOS&MobileApp=yaguApp" +
                        "&baseYm=%s&areaCd=%d&signguCd=%d&numOfRows=%d&pageNo=%d&_type=json",
                props.getRelate().getBaseUrl(), key, baseYm,
                s.getAreaCd(), s.getSignguCd(), rows, page);
        return rt.getForObject(URI.create(url), String.class);
    }
}
