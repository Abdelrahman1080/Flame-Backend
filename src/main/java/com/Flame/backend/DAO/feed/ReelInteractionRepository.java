package com.Flame.backend.DAO.feed;

import com.Flame.backend.entities.feed.ReelInteraction;
import com.Flame.backend.entities.user.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReelInteractionRepository extends JpaRepository<ReelInteraction, Long> {
    
    @Query("SELECT ri FROM ReelInteraction ri JOIN FETCH ri.reel WHERE ri.customer = :customer ORDER BY ri.createdAt DESC")
    List<ReelInteraction> findRecentByCustomer(@Param("customer") Customer customer, Pageable pageable);
}
