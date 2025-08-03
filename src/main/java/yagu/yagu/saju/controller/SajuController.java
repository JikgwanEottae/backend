package yagu.yagu.saju.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yagu.yagu.common.response.ApiResponse;
import yagu.yagu.saju.dto.SajuRequestDto;
import yagu.yagu.saju.dto.SajuResponseDto;
import yagu.yagu.saju.service.SajuService;

@RestController
@RequestMapping("/api/saju")
public class SajuController {
    private final SajuService sajuService;

    @Autowired
    public SajuController(SajuService sajuService) {
        this.sajuService = sajuService;
    }

    @PostMapping("/reading")
    public ResponseEntity<ApiResponse<SajuResponseDto>> getReading(@RequestBody SajuRequestDto req) {
        try {
            SajuResponseDto result = sajuService.getReading(req);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<SajuResponseDto>builder()
                            .result(false)
                            .httpCode(400)
                            .data(null)
                            .message(ex.getMessage())
                            .build()
            );
        }
    }
}
