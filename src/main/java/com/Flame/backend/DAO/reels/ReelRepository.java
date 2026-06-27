package com.Flame.backend.DAO.reels;

import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.ReelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReelRepository extends JpaRepository<Reel, Long> {

    // Existing — used by ReelServiceImpl.getAll() and ReelServiceImpl.getById()
    List<Reel> findByCreator(Customer creator);
    List<Reel> findByStatus(ReelStatus status);

    // New — used by FeedServiceImpl for paginated feed queries
    Page<Reel> findByStatusOrderByCreatedAtDesc(ReelStatus status, Pageable pageable);

    // New — used by FeedServiceImpl to exclude seen reels
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Reel r WHERE r.status = :status " +
            "AND NOT EXISTS (SELECT 1 FROM com.Flame.backend.entities.feed.CustomerSeenReel s WHERE s.reel = r AND s.customer = :customer) " +
            "ORDER BY r.createdAt DESC")
    Page<Reel> findUnseenByStatusOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("status") ReelStatus status, 
                                                      @org.springframework.data.repository.query.Param("customer") Customer customer, 
                                                      Pageable pageable);
}