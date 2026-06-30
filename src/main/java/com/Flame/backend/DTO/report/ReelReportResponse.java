package com.Flame.backend.DTO.report;

import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReelReportResponse {
    private Long id;
    private Long reelId;
    private String reelCaption;
    private String videoUrl;
    private UserResponse reporter;
    private String reason;
    private ReportStatus status;
    private String adminNote;
    private String reviewedByAdmin;
    private LocalDateTime reportedAt;
    private LocalDateTime reviewedAt;
}