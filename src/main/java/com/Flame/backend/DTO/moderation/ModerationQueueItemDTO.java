package com.Flame.backend.DTO.moderation;

import com.Flame.backend.enums.ReelStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationQueueItemDTO {

    private Long moderationId;
    private Long reelId;
    private String reelCaption;
    private String videoUrl;
    private String thumbnailUrl;
    private String creatorEmail;
    private LocalDateTime reelCreatedAt;

    // AI verdict
    private boolean aiFlagged;
    private double aiConfidenceScore;
    private String aiReason;
    private String aiViolationCategories;

    /** Content labels e.g. "EDUCATION, SCIENCE, LECTURE" */
    private String aiContentLabels;

    private LocalDateTime aiScannedAt;

    // Current status
    private ReelStatus status;

    // Admin review
    private String reviewedByAdmin;
    private String adminNote;
    private LocalDateTime adminReviewedAt;
}