package com.Flame.backend.DTO.report;

import lombok.Data;

@Data
public class ReportReelRequest {
    private String reason; // e.g. "Inappropriate content", "Spam", "Violence"
}
