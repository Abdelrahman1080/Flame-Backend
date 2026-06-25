package com.Flame.backend.entities.Reels;

import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.ReelStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caption;

    private String videoUrl;

    private String thumbnailUrl;

    private Integer durationSeconds;

    // ── Moderation fields (NEW) ───────────────────────────────────────────────

    /**
     * Lifecycle status of the reel.
     * Starts as PENDING_REVIEW on upload.
     * Updated to APPROVED or AI_FLAGGED after scan.
     * Admin can then set to REJECTED.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReelStatus status = ReelStatus.PENDING_REVIEW;

    /** Back-reference to moderation result (created after scan completes) */
    @OneToOne(mappedBy = "reel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ModerationResult moderationResult;

    /** Timestamp of when the reel was uploaded */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ── Existing fields (unchanged) ───────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Customer creator;

    @ManyToMany
    @Builder.Default
    @JoinTable(
            name = "reel_likes",
            joinColumns = @JoinColumn(name = "reel_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    private Set<Customer> likes = new HashSet<>();

    @ManyToMany
    @Builder.Default
    @JoinTable(
            name = "reel_saves",
            joinColumns = @JoinColumn(name = "reel_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    private Set<Customer> savedBy = new HashSet<>();

    @OneToMany(mappedBy = "reel", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    private String preferences;

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Returns true only when the reel is safe to show to normal users */
    public boolean isPubliclyVisible() {
        return this.status == ReelStatus.APPROVED;
    }
}