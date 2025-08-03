package yagu.yagu.saju.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import yagu.yagu.saju.config.FastApiProperties;
import yagu.yagu.saju.dto.SajuRequestDto;
import yagu.yagu.saju.dto.SajuResponseDto;

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
            ResponseEntity<SajuResponseDto> resp = restTemplate.postForEntity(
                    props.getBaseUrl() + "/reading",
                    req,
                    SajuResponseDto.class
            );
            return resp.getBody();
        } catch (HttpClientErrorException.BadRequest ex) {
            throw new IllegalArgumentException("잘못된 입력 형식입니다: " + ex.getResponseBodyAsString());
        }
    }
}
