package yagu.yagu.tourapi.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TourAttractionListResponseDto {
    private String stadium;
    private List<TourAttractionResponseDto> attractions;
}
