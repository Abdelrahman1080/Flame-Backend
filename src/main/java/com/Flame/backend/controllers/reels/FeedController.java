package com.Flame.backend.controllers.reels;

import com.Flame.backend.DTO.reels.FeedPageDTO;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.services.feed.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Personalised recommendation feed endpoint.
 *
 * <pre>
 * GET /api/feed?page=0&size=10
 * </pre>
 *
 * <p>Authentication is required. Only Customer-role users may access this
 * endpoint — Admin users receive a 403 because they are not Customer entities
 * and therefore have no preference profile to personalise the feed with.
 *
 * <p>This controller does NOT change the existing {@code GET /api/reels}
 * endpoint, which remains available and unchanged.
 *
 * <p>The feed only returns reels with {@code status == APPROVED}.
 * PENDING_REVIEW, AI_FLAGGED, and REJECTED reels are never included.
 */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * Returns a personalised, scored, paginated reel feed.
     *
     * @param page zero-based page index (default 0)
     * @param size reels per page (default 10, clamped to max 50)
     * @param authentication injected by Spring Security from the JWT token
     * @return {@link FeedPageDTO} with scored reels and pagination metadata
     */
    @GetMapping
    public FeedPageDTO getFeed(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cursor,
            Authentication authentication
    ) {
        // Guard: only Customer-role users have a preference profile
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Customer)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The feed is only available for customer accounts.");
        }

        String userEmail = authentication.getName();
        return feedService.getFeed(userEmail, page, size, cursor);
    }
}
