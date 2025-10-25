package yagu.yagu.tourapi.repository;

import yagu.yagu.tourapi.domain.TourAttraction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TourAttractionRepository extends JpaRepository<TourAttraction, Long> {
    List<TourAttraction> findByStadiumOrderByRankingAsc(String stadium);
}