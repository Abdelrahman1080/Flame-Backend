package com.Flame.backend.services.reels;

import com.Flame.backend.DAO.reels.ReelReportRepository;
import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.DTO.report.AdminReportReviewRequest;
import com.Flame.backend.DTO.report.ReelReportResponse;
import com.Flame.backend.DTO.report.ReportReelRequest;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.Reels.ReelReport;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.enums.ReelStatus;
import com.Flame.backend.enums.ReportStatus;
import com.Flame.backend.services.reelsModeration.GcsFileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReelReportService {

    private final ReelReportRepository reelReportRepository;
    private final ReelRepository reelRepository;
    private final UserRepository userRepository;
    private final GcsFileUploadService gcsFileUploadService;

    // ── Mapper ───────────────────────────────────────────────────────────────

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getProfileUrl(),
                user.getRole()
        );
    }

    private ReelReportResponse toResponse(ReelReport report) {
        return new ReelReportResponse(
                report.getId(),
                report.getReel().getId(),
                report.getReel().getCaption(),
                report.getReel().getVideoUrl(),
                toUserResponse(report.getReporter()),
                report.getReason(),
                report.getStatus(),
                report.getAdminNote(),
                report.getReviewedByAdmin(),
                report.getReportedAt(),
                report.getReviewedAt()
        );
    }

    // ── User: Report a reel ──────────────────────────────────────────────────

    @Transactional
    public ReelReportResponse reportReel(Integer currentUserId, Long reelId, ReportReelRequest request) {
        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        // Prevent duplicate reports from same user
        if (reelReportRepository.existsByReelIdAndReporterId(reelId, currentUserId)) {
            throw new RuntimeException("You have already reported this reel.");
        }

        // Prevent reporting your own reel
        if (reel.getCreator().getId().equals(currentUserId)) {
            throw new RuntimeException("You cannot report your own reel.");
        }

        User reporter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));


        ReelReport report = ReelReport.builder()
                .reel(reel)
                .reporter(reporter)
                .reason(request.getReason())
                .build();

        return toResponse(reelReportRepository.save(report));
    }

    // ── Admin: Get all pending reports ───────────────────────────────────────

    public List<ReelReportResponse> getPendingReports() {
        return reelReportRepository.findByStatus(ReportStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin: Get all reports ────────────────────────────────────────────────

    public List<ReelReportResponse> getAllReports() {
        return reelReportRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin: Get reports for a specific reel ────────────────────────────────

    public List<ReelReportResponse> getReportsByReel(Long reelId) {
        return reelReportRepository.findByReelId(reelId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin: Review a report ────────────────────────────────────────────────

    @Transactional
    public ReelReportResponse reviewReport(Long reportId, AdminReportReviewRequest request) {
        if (request.getDecision() != ReportStatus.REVIEWED && request.getDecision() != ReportStatus.REMOVED) {
            throw new RuntimeException("Decision must be REVIEWED (keep) or REMOVED (delete reel).");
        }

        ReelReport report = reelReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        report.setStatus(request.getDecision());
        report.setAdminNote(request.getAdminNote());
        report.setReviewedByAdmin(adminEmail);
        report.setReviewedAt(LocalDateTime.now());
        reelReportRepository.save(report);

        Reel reel = report.getReel();

        if (request.getDecision() == ReportStatus.REMOVED) {
            // Delete video from GCS and remove reel from DB

            //remove all reports for that reel
            List<ReelReport>       reportsForReel = reelReportRepository.findByReelId(reel.getId());
            reelReportRepository.deleteAll(reportsForReel);

            gcsFileUploadService.deleteFile(reel.getVideoUrl());

            reelRepository.delete(reel);
        } else {
            // Admin reviewed and decided to keep — restore to APPROVED
            reel.setStatus(ReelStatus.APPROVED);
            reelRepository.save(reel);
        }

        return toResponse(report);
    }
}
