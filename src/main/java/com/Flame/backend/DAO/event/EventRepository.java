package com.Flame.backend.DAO.event;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Provider;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    interface EventRecommendationProjection {
        Long getId();
        String getTitle();
        String getDescription();
        String getLocation();
        String getCategory();
        String getImageUrl();
        Integer getCapacity();
        Integer getScore();
    }

    List<Event> findByProvider(Provider provider);
    List<Event> findByProvider_Id(Integer providerId);

    @Query(value = """
            SELECT
                e.id AS id,
                e.title AS title,
                e.description AS description,
                e.location AS location,
                e.category AS category,
                e.image_url AS imageUrl,
                e.capacity AS capacity,
                CASE
                    WHEN TRIM(COALESCE(:preferences, '')) = '' THEN 0
                    ELSE COALESCE((
                        SELECT SUM(
                            (CASE WHEN LOWER(COALESCE(e.category, '')) LIKE CONCAT('%', pref, '%') THEN 5 ELSE 0 END) +
                            (CASE WHEN LOWER(COALESCE(e.title, '')) LIKE CONCAT('%', pref, '%') THEN 4 ELSE 0 END) +
                            (CASE WHEN LOWER(COALESCE(e.description, '')) LIKE CONCAT('%', pref, '%') THEN 3 ELSE 0 END) +
                            (CASE WHEN LOWER(COALESCE(e.location, '')) LIKE CONCAT('%', pref, '%') THEN 2 ELSE 0 END)
                        )
                        FROM unnest(string_to_array(LOWER(:preferences), ',')) AS pref
                    ), 0)
                END AS score
            FROM event e
            ORDER BY score DESC, e.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<EventRecommendationProjection> findRecommendedEvents(
            @Param("preferences") String preferences,
            @Param("limit") int limit
    );
}
