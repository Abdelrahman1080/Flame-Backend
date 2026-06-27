package com.Flame.backend.DTO.reels;

import lombok.*;

import java.util.List;

/**
 * Paginated response envelope for the personalised reel feed.
 *
 * <p>Used by {@code GET /api/feed}. Individual items use the existing
 * {@link ReelResponseDTO} shape — nothing breaking here.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPageDTO {

    /** The scored, ordered reels for this page. */
    private List<ReelResponseDTO> content;

    /** Zero-based page index (matches the ?page query param). */
    private int page;

    /** Number of reels requested per page. */
    private int size;

    /** Total number of approved reels available across all pages. */
    private long totalElements;

    /** Convenience flag — true when there are more pages after this one. */
    private boolean hasNext;

    /** Opaque cursor for the next batch of results (Phase 2B). */
    private String nextCursor;
}
