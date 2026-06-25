package com.Flame.backend.entities.Reels;

import com.Flame.backend.enums.ReelStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "reel_id", nullable = false, unique = true)
    private Reel reel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReelStatus status;

    @Column(nullable = false)
    private boolean aiFlagged;

    private Double aiConfidenceScore;

    @Column(columnDefinition = "TEXT")
    private String aiReason;

    /** Violation categories: NUDITY, VIOLENCE, ADULT_CONTENT, RACY, NONE */
    private String aiViolationCategories;

    /**
     * Content labels from label detection.
     * e.g. "EDUCATION, SCIENCE, LECTURE" — used for automatic reel categorization.
     */
    @Column(columnDefinition = "TEXT")
    private String aiContentLabels;

    private LocalDateTime aiScannedAt;

    private String reviewedByAdmin;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    private LocalDateTime adminReviewedAt;
}