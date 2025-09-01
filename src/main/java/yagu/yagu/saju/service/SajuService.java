package yagu.yagu.saju.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import yagu.yagu.saju.config.FastApiProperties;
import yagu.yagu.saju.dto.SajuRequestDto;
import yagu.yagu.saju.dto.SajuResponseDto;
import yagu.yagu.saju.dto.TeamName;

import java.util.HashMap;
import java.util.Map;

@Service
public class SajuService {
    private final RestTemplate restTemplate;
    private final FastApiProperties props;

    @Autowired
    public SajuService(RestTemplate restTemplate, FastApiProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    public SajuResponseDto getReading(SajuRequestDto req) {
        try {
            // 1) 팀명 변환
            String stdTeam = TeamName.toStandardOrThrow(req.getTeamName());

            // 2) birth_date + time 조합
            String combinedBirth = buildFastApiBirth(req.getBirthDate(), req.getTime());

            // 3) FastAPI 요청 데이터
            Map<String, Object> fastApiReq = new HashMap<>();
            fastApiReq.put("birth_date", combinedBirth);
            fastApiReq.put("gender", req.getGender());
            fastApiReq.put("team_name", stdTeam);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(fastApiReq, headers);

            // 4) FastAPI 호출
            ResponseEntity<SajuResponseDto> resp = restTemplate.postForEntity(
                    props.getBaseUrl() + "/reading",
                    fastApiReq,
                    SajuResponseDto.class
            );
            return resp.getBody();

        } catch (HttpClientErrorException.BadRequest ex) {
            throw new IllegalArgumentException("잘못된 입력 형식입니다: " + ex.getResponseBodyAsString());
        }
    }

    private String buildFastApiBirth(String yyyymmdd, Integer time) {
        if (yyyymmdd == null || !yyyymmdd.matches("^\\d{8}$")) {
            throw new IllegalArgumentException("birth_date는 YYYYMMDD 8자리여야 합니다: " + yyyymmdd);
        }

        // 시간이 null → 시간모름
        if (time == null) {
            return yyyymmdd + "시간모름";
        }

        // 0~23 범위 검증
        if (time < 0 || time > 23) {
            throw new IllegalArgumentException("time은 0~23 사이여야 합니다: " + time);
        }

        return yyyymmdd + String.format("%02d", time);
    }

}
