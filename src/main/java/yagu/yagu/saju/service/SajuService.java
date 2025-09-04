package yagu.yagu.saju.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import yagu.yagu.saju.config.FastApiProperties;
import yagu.yagu.saju.dto.SajuRequestDto;
import yagu.yagu.saju.dto.SajuResponseDto;
import yagu.yagu.saju.dto.TeamName;

import java.util.Map;

@Service
public class SajuService {
    private final WebClient fastApiWebClient;
    private final FastApiProperties props;

    @Autowired
    public SajuService(WebClient fastApiWebClient, FastApiProperties props) {
        this.fastApiWebClient = fastApiWebClient;
        this.props = props;
    }

    public SajuResponseDto getReading(SajuRequestDto req) {
        try {
            // 1) 팀명 변환
            String stdTeam = TeamName.toStandardOrThrow(req.getTeamName());

            // 2) birth_date + time 조합
            String combinedBirth = buildFastApiBirth(req.getBirthDate(), req.getTime());

            // 3) FastAPI 요청 데이터
            Map<String, Object> body = Map.of(
                    "birth_date", combinedBirth,
                    "gender", req.getGender(),
                    "team_name", stdTeam
            );

            // 4) 호출
            return fastApiWebClient.post()
                    .uri("/reading")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(SajuResponseDto.class)
                    .block();

        } catch (WebClientResponseException.UnprocessableEntity ex) { // 422
            // FastAPI(pydantic) 검증 실패 응답 본문 노출
            throw new IllegalArgumentException("FastAPI 422: " + ex.getResponseBodyAsString(), ex);
        } catch (WebClientResponseException.BadRequest ex) { // 400
            throw new IllegalArgumentException("잘못된 입력 형식입니다: " + ex.getResponseBodyAsString(), ex);
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
