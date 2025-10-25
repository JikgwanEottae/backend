package yagu.yagu.tourapi.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourAttraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 구장명 */
    @Column(nullable = false, length = 200)
    private String stadium;

    /** 순위 */
    private Integer ranking;

    /** 연관관광지명 */
    @Column(nullable = false, length = 200)
    private String attraction;

    /** 연관관광지시도명 */
    @Column(length = 100)
    private String city;

    /** 연관관광지시군구명 */
    @Column(length = 100)
    private String district;

    /** 구분 */
    @Column(length = 100)
    private String category;
}