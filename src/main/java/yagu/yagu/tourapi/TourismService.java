package yagu.yagu.tourapi;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class TourismService {
    private final TourismProperties props;
    private final RestTemplate rt;

    public TourismService(TourismProperties props, RestTemplate rt) {
        this.props = props;
        this.rt = rt;
    }

    public String getAreaListJson(Region region, int contentTypeId, int numOfRows, int pageNo) {
        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(props.getArea().getBaseUrl() + "/areaBasedList2")
                .queryParam("serviceKey", props.getArea().getServiceKey())
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "TestApp")
                .queryParam("arrange", "C")
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("areaCode", region.getAreaCode())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("_type", "json");
        if (region.getSigunguCode() != null) {
            b.queryParam("sigunguCode", region.getSigunguCode());
        }
        URI uri = b.build().encode().toUri();
        return rt.getForObject(uri, String.class);
    }

    public String getRelatedListJson(Stadium stadium, int numOfRows, int pageNo) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(props.getRelate().getBaseUrl() + "/areaBasedList1")
                .queryParam("serviceKey", props.getRelate().getServiceKey())
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "ETC")
                .queryParam("baseYm", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")))
                .queryParam("areaCd", stadium.getAreaCd())
                .queryParam("signguCd", stadium.getSignguCd())
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("_type", "json")
                .build().encode().toUri();
        return rt.getForObject(uri, String.class);
    }
}
