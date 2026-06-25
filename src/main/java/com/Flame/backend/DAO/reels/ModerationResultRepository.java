package com.Flame.backend.DAO.reels;

import com.Flame.backend.entities.Reels.ModerationResult;
import com.Flame.backend.enums.ReelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModerationResultRepository extends JpaRepository<ModerationResult, Long> {

    Optional<ModerationResult> findByReelId(Long reelId);

    List<ModerationResult> findByStatus(ReelStatus status);

    List<ModerationResult> findByAiFlaggedTrue();
}