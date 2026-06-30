package com.Flame.backend.entities.Reels;

import com.Flame.backend.entities.user.User;
import com.Flame.backend.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reel_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReelReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reel_id", nullable = false)
    private Reel reel;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    private String reason;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    private String adminNote;
    private String reviewedByAdmin;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;

    @PrePersist
    public void prePersist() {
        this.reportedAt = LocalDateTime.now();
        this.status = ReportStatus.PENDING;
    }
}
