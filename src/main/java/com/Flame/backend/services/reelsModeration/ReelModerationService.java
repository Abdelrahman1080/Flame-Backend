package com.Flame.backend.services.reelsModeration;

import com.Flame.backend.DTO.moderation.AdminReviewRequestDTO;
import com.Flame.backend.DTO.moderation.AiModerationResponse;
import com.Flame.backend.DTO.moderation.ModerationQueueItemDTO;
import com.Flame.backend.entities.Reels.ModerationResult;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.enums.ReelStatus;
import com.Flame.backend.DAO.reels.ModerationResultRepository;
import com.Flame.backend.DAO.reels.ReelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReelModerationService {

    private final GoogleVideoModerationService googleVideoModerationService;
    private final GcsUploadService gcsUploadService;
    private final ModerationResultRepository moderationResultRepository;
    private final ReelRepository reelRepository;

    private static final String LOCAL_UPLOADS_PATH = "uploads/";

    @Async
    @Transactional
    public void scanAndPersistAsync(Reel reel, String localFileName) {
        log.info("Background moderation scan started for reel id={}", reel.getId());
        String gcsFileName = "reels/" + localFileName;
        String gcsUri = null;

        try {
            String localFilePath = LOCAL_UPLOADS_PATH + localFileName;
            gcsUri = gcsUploadService.uploadVideoToGcs(localFilePath, gcsFileName);

            AiModerationResponse aiResult = googleVideoModerationService.moderateVideo(gcsUri);

            ReelStatus newStatus = aiResult.isFlagged()
                    ? ReelStatus.AI_FLAGGED
                    : ReelStatus.APPROVED;

            reel.setStatus(newStatus);
            reelRepository.save(reel);

            ModerationResult record = ModerationResult.builder()
                    .reel(reel)
                    .status(newStatus)
                    .aiFlagged(aiResult.isFlagged())
                    .aiConfidenceScore(aiResult.getConfidenceScore())
                    .aiReason(aiResult.getReason())
                    .aiViolationCategories(aiResult.getViolationCategories())
                    .aiContentLabels(aiResult.getContentLabels())
                    .aiScannedAt(LocalDateTime.now())
                    .build();

            moderationResultRepository.save(record);
            log.info("Reel id={} — status={}, labels={}", reel.getId(), newStatus, aiResult.getContentLabels());

        } catch (Exception ex) {
            log.error("Moderation scan failed for reel id={}: {}", reel.getId(), ex.getMessage());
            reel.setStatus(ReelStatus.APPROVED);
            reelRepository.save(reel);
        } finally {
            if (gcsUri != null) {
                gcsUploadService.deleteFromGcs(gcsFileName);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ModerationQueueItemDTO> getFlaggedQueue() {
        return moderationResultRepository.findByStatus(ReelStatus.AI_FLAGGED)
                .stream().map(this::toQueueItemDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ModerationQueueItemDTO> getAllModerationRecords() {
        return moderationResultRepository.findAll()
                .stream().map(this::toQueueItemDTO).collect(Collectors.toList());
    }

    @Transactional
    public ModerationQueueItemDTO adminReview(Long reelId, AdminReviewRequestDTO request) {
        if (request.getDecision() != ReelStatus.APPROVED && request.getDecision() != ReelStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED.");
        }
        ModerationResult record = moderationResultRepository.findByReelId(reelId)
                .orElseThrow(() -> new IllegalArgumentException("No moderation record for reel id=" + reelId));

        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        record.setStatus(request.getDecision());
        record.setReviewedByAdmin(adminEmail);
        record.setAdminNote(request.getNote());
        record.setAdminReviewedAt(LocalDateTime.now());
        moderationResultRepository.save(record);

        Reel reel = record.getReel();
        reel.setStatus(request.getDecision());
        reelRepository.save(reel);

        log.info("Admin '{}' set reel id={} to {}", adminEmail, reelId, request.getDecision());
        return toQueueItemDTO(record);
    }

    private ModerationQueueItemDTO toQueueItemDTO(ModerationResult r) {
        Reel reel = r.getReel();
        return ModerationQueueItemDTO.builder()
                .moderationId(r.getId())
                .reelId(reel.getId())
                .reelCaption(reel.getCaption())
                .videoUrl(reel.getVideoUrl())
                .thumbnailUrl(reel.getThumbnailUrl())
                .creatorEmail(reel.getCreator() != null ? reel.getCreator().getEmail() : "unknown")
                .reelCreatedAt(reel.getCreatedAt())
                .aiFlagged(r.isAiFlagged())
                .aiConfidenceScore(r.getAiConfidenceScore() != null ? r.getAiConfidenceScore() : 0.0)
                .aiReason(r.getAiReason())
                .aiViolationCategories(r.getAiViolationCategories())
                .aiContentLabels(r.getAiContentLabels())
                .aiScannedAt(r.getAiScannedAt())
                .status(r.getStatus())
                .reviewedByAdmin(r.getReviewedByAdmin())
                .adminNote(r.getAdminNote())
                .adminReviewedAt(r.getAdminReviewedAt())
                .build();
    }
}