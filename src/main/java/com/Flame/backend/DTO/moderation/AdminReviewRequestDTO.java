package com.Flame.backend.DTO.moderation;

import com.Flame.backend.enums.ReelStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body sent by admin when approving or rejecting a flagged reel.
 * decision must be either APPROVED or REJECTED.
 */
@Getter
@Setter
public class AdminReviewRequestDTO {

    @NotNull(message = "decision is required: APPROVED or REJECTED")
    private ReelStatus decision;

    /** Optional note explaining the admin's decision (stored for audit trail) */
    private String note;
}
