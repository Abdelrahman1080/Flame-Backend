package com.Flame.backend.DAO.reels;

import com.Flame.backend.entities.Reels.ReelReport;
import com.Flame.backend.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReelReportRepository extends JpaRepository<ReelReport, Long> {
    List<ReelReport> findByStatus(ReportStatus status);
    List<ReelReport> findByReelId(Long reelId);
    Optional<ReelReport> findByReelIdAndReporterId(Long reelId, Integer reporterId);
    boolean existsByReelIdAndReporterId(Long reelId, Integer reporterId);
}
