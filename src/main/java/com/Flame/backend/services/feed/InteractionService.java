package com.Flame.backend.services.feed;

import com.Flame.backend.DTO.feed.InteractionRequestDTO;

public interface InteractionService {
    void recordInteraction(Long reelId, InteractionRequestDTO request, String userEmail);
}
