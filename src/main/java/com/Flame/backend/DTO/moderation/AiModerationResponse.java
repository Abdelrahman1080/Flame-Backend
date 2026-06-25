package com.Flame.backend.DTO.moderation;

import lombok.*;

/**
 * Parsed result from Google Video Intelligence API scan.
 * Used by ReelModerationService to persist into ModerationResult entity.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModerationResponse {

    /** True when the video violates community guidelines. */
    private boolean flagged;

    /** Confidence score of the violation (0.0 – 1.0). */
    private double confidenceScore;

    /**
     * Human-readable explanation.
     * e.g. "Video contains flagged content: nudity, violence."
     */
    private String reason;

    /**
     * Comma-separated violation categories.
     * Possible values: NUDITY, VIOLENCE, ADULT_CONTENT, RACY, NONE
     */
    private String violationCategories;

    /**
     * Comma-separated content labels detected in the video.
     * e.g. "EDUCATION, SCIENCE, LECTURE, CLASSROOM"
     * Used for automatic categorization of the reel.
     */
    private String contentLabels;
}