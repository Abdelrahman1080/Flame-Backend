package com.Flame.backend.services.feed;

import com.Flame.backend.DTO.reels.FeedPageDTO;

/**
 * Contract for the personalised recommendation feed.
 *
 * <p>Follows the same interface-per-service pattern used in the rest of the
 * codebase (e.g., {@code ReelService} / {@code ReelServiceImpl}).
 */
public interface FeedService {

    /**
     * Returns a scored, paginated feed of approved reels personalised for the
     * authenticated customer identified by {@code userEmail}.
     *
     * <p>Only reels with {@code status == APPROVED} are included.
     * Pending, flagged, and rejected reels are never returned.
     *
     * @param userEmail email of the authenticated customer (from JWT)
     * @param page      zero-based page index
     * @param size      page size (clamped to [1, 50] by the implementation)
     * @return paginated scored feed
     */
    FeedPageDTO getFeed(String userEmail, int page, int size, String cursor);
}
