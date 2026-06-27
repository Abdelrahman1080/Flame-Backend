package com.Flame.backend.controllers.feed;

import com.Flame.backend.DTO.feed.InteractionRequestDTO;
import com.Flame.backend.services.feed.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed/reels")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/{reelId}/interaction")
    public ResponseEntity<Void> recordInteraction(
            @PathVariable Long reelId,
            @Valid @RequestBody InteractionRequestDTO request,
            Authentication authentication) {

        // Validate customer role (Admin should be forbidden via SecurityConfig, but extra safety check here)
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return ResponseEntity.status(403).build();
        }

        interactionService.recordInteraction(reelId, request, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
