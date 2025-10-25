package yagu.yagu.tourapi.dto;

import yagu.yagu.tourapi.domain.TourAttraction;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TourAttractionResponseDto {
    private Integer ranking; // 순위
    private String attraction;// 연관관광지명
    private String city; // 연관관광지시도명
    private String district; // 연관관광지시군구명
    private String category; // 구분

    public static TourAttractionResponseDto of(TourAttraction s) {
        return TourAttractionResponseDto.builder()
                .ranking(s.getRanking())
                .attraction(s.getAttraction())
                .city(s.getCity())
                .district(s.getDistrict())
                .category(s.getCategory())
                .build();
    }
}