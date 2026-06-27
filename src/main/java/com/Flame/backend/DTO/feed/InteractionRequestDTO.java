package com.Flame.backend.DTO.feed;

import com.Flame.backend.enums.InteractionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InteractionRequestDTO {

    @NotNull(message = "interactionType is required")
    private InteractionType interactionType;

    private Long watchDurationMs;
    private Long reelDurationMs;
}
