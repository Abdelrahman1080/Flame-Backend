package com.Flame.backend.services.feed;

import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DTO.reels.FeedPageDTO;
import com.Flame.backend.DTO.reels.ReelResponseDTO;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.ReelStatus;
import com.Flame.backend.mappers.ReelMapper;
import com.Flame.backend.DAO.feed.ReelInteractionRepository;
import com.Flame.backend.entities.feed.ReelInteraction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Implements the personalised recommendation feed.
 *
 * <h3>Safety guarantees</h3>
 * <ul>
 *   <li>Only reels with {@code status == APPROVED} are ever fetched or returned.</li>
 *   <li>PENDING_REVIEW, AI_FLAGGED, and REJECTED reels are excluded at the database level.</li>
 *   <li>This service does NOT touch any moderation service, does NOT call
 *       {@code scanAndPersistAsync()}, and does NOT write to moderation results.</li>
 *   <li>{@code ModerationResult.aiContentLabels} is read via {@link FeedScoringService}
 *       and is never modified.</li>
 * </ul>
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Fetch a window of approved reels from the DB, ordered by newest first.</li>
 *   <li>Delegate scoring to {@link FeedScoringService}.</li>
 *   <li>Re-sort by score, then slice for the requested page.</li>
 *   <li>Map to {@link ReelResponseDTO} using the existing {@link ReelMapper}.</li>
 * </ol>
 *
 * <p>The page-then-score approach means the feed is slightly biased toward fresh
 * content (the DB window is already recency-capped), which is intentional for
 * Phase 1 and avoids loading the entire table into memory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private final ReelRepository reelRepository;
    private final CustomerRepository customerRepository;
    private final FeedScoringService feedScoringService;
    private final ReelInteractionRepository interactionRepository;

    /** Maximum allowed page size — prevents abuse. */
    private static final int MAX_SIZE = 50;

    /**
     * We fetch a wider window from the DB than the page size so the scorer has
     * enough candidates to rank well. Window = 5× the requested size, min 100.
     */
    private static final int SCORING_WINDOW_MULTIPLIER = 5;

    @Override
    public FeedPageDTO getFeed(String userEmail, int page, int size, String cursor) {
        // Clamp page size
        int safeSize = Math.max(1, Math.min(size, MAX_SIZE));
        int safePage = Math.max(0, page);

        // Decode cursor if present
        if (cursor != null && !cursor.isBlank()) {
            try {
                String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
                if (decoded.startsWith("page:")) {
                    safePage = Integer.parseInt(decoded.substring(5));
                }
            } catch (Exception e) {
                log.warn("Failed to decode cursor: {}", cursor);
            }
        }

        // Load customer
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + userEmail));

        // ── Phase 1: Fetch candidate window from DB ───────────────────────────
        int windowSize = 100;
        Pageable windowPageable = PageRequest.of(0, windowSize);

        Page<Reel> dbPage = reelRepository.findUnseenByStatusOrderByCreatedAtDesc(
                ReelStatus.APPROVED, customer, windowPageable);

        long totalApproved = dbPage.getTotalElements();
        List<Reel> candidates = dbPage.getContent();

        log.debug("Feed for user={}: {} unseen approved reels in window, page={}, size={}",
                userEmail, candidates.size(), safePage, safeSize);

        // Fetch recent interactions to infer behavioral labels
        List<ReelInteraction> recentInteractions = interactionRepository.findRecentByCustomer(customer, PageRequest.of(0, 50));

        // ── Phase 2: Compose Feed (70/20/10) ──────────────────────────────────
        List<Reel> composed = feedScoringService.composeFeed(candidates, customer, recentInteractions);

        // ── Phase 3: Slice the requested page from the composed list ──────────
        int fromIndex = safePage * safeSize;
        int toIndex   = Math.min(fromIndex + safeSize, composed.size());

        List<Reel> pageReels;
        if (fromIndex >= composed.size()) {
            pageReels = List.of();
        } else {
            pageReels = composed.subList(fromIndex, toIndex);
        }

        // ── Phase 4: Map to DTOs & Encode Next Cursor ─────────────────────────
        List<ReelResponseDTO> dtos = pageReels.stream()
                .map(reel -> ReelMapper.toDTO(reel, customer))
                .collect(Collectors.toList());

        boolean hasNext = toIndex < composed.size() || (long) ((safePage + 1) * safeSize) < totalApproved;
        
        String nextCursor = null;
        if (hasNext) {
            nextCursor = Base64.getEncoder().encodeToString(("page:" + (safePage + 1)).getBytes(StandardCharsets.UTF_8));
        }

        return FeedPageDTO.builder()
                .content(dtos)
                .page(safePage)
                .size(dtos.size())
                .totalElements(totalApproved)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }
}
