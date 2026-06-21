package com.Flame.backend.DAO.reels;

import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReelRepository extends JpaRepository<Reel, Long> {

    List<Reel> findByCreator(Customer creator);

}