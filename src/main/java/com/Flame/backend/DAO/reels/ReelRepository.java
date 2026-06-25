package com.Flame.backend.DAO.reels;

import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.ReelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ReelRepository extends JpaRepository<Reel, Long> {

    List<Reel> findByCreator(Customer creator);
    List<Reel> findByStatus(ReelStatus status);

}