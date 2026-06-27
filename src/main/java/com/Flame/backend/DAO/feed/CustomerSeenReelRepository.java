package com.Flame.backend.DAO.feed;

import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.feed.CustomerSeenReel;
import com.Flame.backend.entities.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerSeenReelRepository extends JpaRepository<CustomerSeenReel, Long> {
    boolean existsByCustomerAndReel(Customer customer, Reel reel);
}
