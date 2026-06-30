package com.Flame.backend.enums;

public enum ReportStatus {
    PENDING,     // just reported, waiting for admin review
    REVIEWED,    // admin looked at it and kept the reel
    REMOVED      // admin removed the reel
}
