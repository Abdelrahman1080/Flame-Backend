package com.Flame.backend.DTO.report;

import com.Flame.backend.enums.ReportStatus;
import lombok.Data;

@Data
public class AdminReportReviewRequest {
    private ReportStatus decision; // REVIEWED (keep) or REMOVED (delete reel)
    private String adminNote;
}