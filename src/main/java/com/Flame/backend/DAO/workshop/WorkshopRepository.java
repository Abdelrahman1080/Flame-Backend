package com.Flame.backend.DAO.workshop;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Flame.backend.entities.user.Provider;
import com.Flame.backend.entities.workshop.Workshop;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    interface WorkshopRecommendationProjection {
        Long getId();
        String getTitle();
        String getDescription();
        String getLocation();
        String getCategory();
        String getImageUrl();
        Integer getCapacity();
        Integer getScore();
    }

    List<Workshop> findByProvider(Provider provider);
    List<Workshop> findByProvider_Id(Integer providerId);

        @Query("""
            SELECT w
            FROM Workshop w
            WHERE (:category IS NULL OR :category = '' OR LOWER(COALESCE(w.category, '')) LIKE LOWER(CONCAT('%', :category, '%')))
              AND (:location IS NULL OR :location = '' OR LOWER(COALESCE(w.location, '')) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:fromDate IS NULL OR COALESCE(w.endDate, w.startDate) >= :fromDate)
              AND (:toDate IS NULL OR w.startDate <= :toDate)
            ORDER BY w.startDate ASC, w.id DESC
            """)
        List<Workshop> searchFiltered(
            @Param("category") String category,
            @Param("location") String location,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
        );

    @Query(value = """
            SELECT
                w.id AS id,
                w.title AS title,
                w.description AS description,
                w.location AS location,
                w.category AS category,
                w.image_url AS imageUrl,
                w.capacity AS capacity,
                CASE
                    WHEN TRIM(COALESCE(:preferences, '')) = '' THEN 0
                    ELSE COALESCE((
                        SELECT SUM(
                            (CASE WHEN LOWER(COALESCE(w.category, '')) LIKE CONCAT('%', pref, '%') THEN 5 ELSE 0 END) +
                            (CASE WHEN LOWER(COALESCE(w.title, '')) LIKE CONCAT('%', pref, '%') THEN 4 ELSE 0 END) +
                            (CASE WHEN LOWER(COALESCE(w.description, '')) LIKE CONCAT('%', pref, '%') THEN 3 ELSE 0 END) +
                            (CASE WHEN LOWER(COALESCE(w.location, '')) LIKE CONCAT('%', pref, '%') THEN 2 ELSE 0 END)
                        )
                        FROM unnest(string_to_array(LOWER(:preferences), ',')) AS pref
                    ), 0)
                END AS score
            FROM workshop w
            ORDER BY score DESC, w.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<WorkshopRecommendationProjection> findRecommendedWorkshops(
            @Param("preferences") String preferences,
            @Param("limit") int limit
    );

}
