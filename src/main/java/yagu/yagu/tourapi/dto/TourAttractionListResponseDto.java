package yagu.yagu.tourapi.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TourAttractionListResponseDto {
    private boolean result;
    private int httpCode;
    private String stadium;
    private List<TourAttractionResponseDto> data;
    private String message;
}
