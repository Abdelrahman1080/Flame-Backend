package com.Flame.backend.enums;

public enum ReelStatus {

    /**
     * Just uploaded — waiting for AI scan.
     */
    PENDING_REVIEW,

    /**
     * AI passed it; visible to all users.
     */
    APPROVED,

    /**
     * AI flagged it; hidden until an admin decides.
     */
    AI_FLAGGED,

    /**
     * Admin rejected it; permanently hidden.
     */
    REJECTED
}
