package com.Flame.backend.services.feed;

import com.Flame.backend.entities.Reels.ModerationResult;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.feed.ReelInteraction;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.InteractionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure scoring logic for the personalised recommendation feed.
 *
 * <h3>Score formula (users WITH preferences)</h3>
 * <pre>
 *   score = preferenceScore * 0.5
 *         + engagementScore  * 0.3
 *         + recencyScore     * 0.2
 * </pre>
 *
 * <h3>Fallback (users with NO preferences)</h3>
 * <pre>
 *   score = engagementScore * 0.6
 *         + recencyScore    * 0.4
 * </pre>
 *
 * <p>This service is READ-ONLY with respect to moderation data.
 * It reads {@link ModerationResult#getAiContentLabels()} but NEVER writes to it.
 * It reads {@link Reel#getStatus()} but NEVER changes it.
 */
@Slf4j
@Service
public class FeedScoringService {

    // ── Score weights ─────────────────────────────────────────────────────────
    private static final double WEIGHT_PREFERENCE  = 0.5;
    private static final double WEIGHT_ENGAGEMENT  = 0.3;
    private static final double WEIGHT_RECENCY     = 0.2;

    // Fallback weights when user has no preferences
    private static final double WEIGHT_FALLBACK_ENGAGEMENT = 0.6;
    private static final double WEIGHT_FALLBACK_RECENCY    = 0.4;

    // Recency decay: half-life in days — a reel uploaded 7 days ago scores ~50%
    private static final double HALF_LIFE_DAYS = 7.0;

    // Comment weight inside the engagement formula (comments worth half a like)
    private static final double COMMENT_WEIGHT = 0.5;

    // Behavioral weights
    private static final double POSITIVE_LABEL_BOOST = 0.1;
    private static final double NEGATIVE_LABEL_PENALTY = 0.2;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Scores and sorts a batch of approved reels for a given customer, taking into account recent interactions.
     */
    public List<Reel> scoreAndSort(List<Reel> reels, Customer customer, List<ReelInteraction> recentInteractions) {
        if (reels == null || reels.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> userPrefs = parsePreferences(customer.getPreferences());
        boolean hasPreferences = !userPrefs.isEmpty();

        // Infer labels from behavior
        Set<String> inferredPositive = new HashSet<>();
        Set<String> inferredNegative = new HashSet<>();

        if (recentInteractions != null) {
            for (ReelInteraction interaction : recentInteractions) {
                Set<String> labels = extractLabels(interaction.getReel());
                if (isPositive(interaction)) {
                    inferredPositive.addAll(labels);
                } else if (isNegative(interaction)) {
                    inferredNegative.addAll(labels);
                }
            }
        }

        // Pre-compute max engagement across the batch for normalization
        double maxEngagement = reels.stream()
                .mapToDouble(this::rawEngagement)
                .max()
                .orElse(1.0);
        // Guard against all-zero engagement batches
        if (maxEngagement == 0.0) maxEngagement = 1.0;

        final double finalMaxEngagement = maxEngagement;

        // Score each reel
        Map<Reel, Double> scores = new LinkedHashMap<>();
        for (Reel reel : reels) {
            double score = computeScore(reel, userPrefs, hasPreferences, finalMaxEngagement, inferredPositive, inferredNegative);
            scores.put(reel, score);
            log.debug("Reel id={} score={:.4f}", reel.getId(), score);
        }

        // Sort descending by score
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Reel, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Composes the final feed with a 70% personalized, 20% trending, 10% exploration split.
     * It interleaves the candidates so that every batch of 10 roughly follows the 7/2/1 ratio.
     */
    public List<Reel> composeFeed(List<Reel> candidates, Customer customer, List<ReelInteraction> recentInteractions) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Reel> available = new ArrayList<>(candidates);

        // 1. Personalized Pool (Sorted by score)
        List<Reel> personalizedPool = new ArrayList<>(scoreAndSort(available, customer, recentInteractions));

        // 2. Trending Pool (Sorted by raw engagement, tie-break by ID)
        List<Reel> trendingPool = new ArrayList<>(available);
        trendingPool.sort((a, b) -> {
            int cmp = Double.compare(rawEngagement(b), rawEngagement(a));
            if (cmp == 0) return Long.compare(b.getId(), a.getId());
            return cmp;
        });

        // 3. Exploration Pool (No negative labels, shuffled)
        Set<String> negativeLabels = new HashSet<>();
        if (recentInteractions != null) {
            for (ReelInteraction interaction : recentInteractions) {
                if (isNegative(interaction)) {
                    negativeLabels.addAll(extractLabels(interaction.getReel()));
                }
            }
        }
        
        List<Reel> explorationPool = available.stream()
                .filter(r -> {
                    Set<String> labels = extractLabels(r);
                    return labels.stream().noneMatch(negativeLabels::contains);
                })
                .collect(Collectors.toList());
        
        // Pseudo-random but deterministic shuffle per customer
        explorationPool.sort((a, b) -> {
            long hashA = a.getId() ^ customer.getId();
            long hashB = b.getId() ^ customer.getId();
            return Long.compare(hashA, hashB);
        });

        // Interleave pools: For every 10 items, take 7 P, 2 T, 1 E
        List<Reel> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        int pIndex = 0;
        int tIndex = 0;
        int eIndex = 0;

        while (result.size() < candidates.size()) {
            // Take up to 7 personalized
            int pCount = 0;
            while (pCount < 7 && pIndex < personalizedPool.size()) {
                Reel r = personalizedPool.get(pIndex++);
                if (seenIds.add(r.getId())) {
                    result.add(r);
                    pCount++;
                }
            }

            // Take up to 2 trending
            int tCount = 0;
            while (tCount < 2 && tIndex < trendingPool.size()) {
                Reel r = trendingPool.get(tIndex++);
                if (seenIds.add(r.getId())) {
                    result.add(r);
                    tCount++;
                }
            }

            // Take up to 1 exploration
            int eCount = 0;
            while (eCount < 1 && eIndex < explorationPool.size()) {
                Reel r = explorationPool.get(eIndex++);
                if (seenIds.add(r.getId())) {
                    result.add(r);
                    eCount++;
                }
            }

            // If we couldn't find any unseen item in this round, break to avoid infinite loop
            if (pCount == 0 && tCount == 0 && eCount == 0) {
                break;
            }
        }

        // Backfill any remaining items just in case
        for (Reel r : personalizedPool) {
            if (seenIds.add(r.getId())) {
                result.add(r);
            }
        }

        return result;
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    private double computeScore(Reel reel,
                                Set<String> userPrefs,
                                boolean hasPreferences,
                                double maxEngagement,
                                Set<String> inferredPositive,
                                Set<String> inferredNegative) {
        double engagementScore = engagementScore(reel, maxEngagement);
        double recencyScore    = recencyScore(reel.getCreatedAt());

        double baseScore;
        if (!hasPreferences) {
            // Fallback: engagement + recency only
            baseScore = engagementScore * WEIGHT_FALLBACK_ENGAGEMENT
                      + recencyScore    * WEIGHT_FALLBACK_RECENCY;
        } else {
            double preferenceScore = preferenceScore(reel, userPrefs);
            baseScore = preferenceScore * WEIGHT_PREFERENCE
                      + engagementScore * WEIGHT_ENGAGEMENT
                      + recencyScore    * WEIGHT_RECENCY;
        }

        // Apply behavioral modifiers
        Set<String> reelLabels = extractLabels(reel);
        int posMatches = 0;
        int negMatches = 0;
        for (String label : reelLabels) {
            if (inferredPositive.contains(label)) posMatches++;
            if (inferredNegative.contains(label)) negMatches++;
        }

        double finalScore = baseScore + (posMatches * POSITIVE_LABEL_BOOST) - (negMatches * NEGATIVE_LABEL_PENALTY);
        return Math.max(0.0, finalScore);
    }

    // ── Individual scorers ────────────────────────────────────────────────────

    private double preferenceScore(Reel reel, Set<String> userPrefs) {
        Set<String> reelLabels = extractLabels(reel);

        if (reelLabels.isEmpty()) {
            return 0.0;
        }

        // Jaccard: |intersection| / |union|
        Set<String> intersection = new HashSet<>(userPrefs);
        intersection.retainAll(reelLabels);

        Set<String> union = new HashSet<>(userPrefs);
        union.addAll(reelLabels);

        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    private double engagementScore(Reel reel, double maxEngagement) {
        double raw = rawEngagement(reel);
        return Math.min(raw / maxEngagement, 1.0);
    }

    private double rawEngagement(Reel reel) {
        long likes    = reel.getLikes()    != null ? reel.getLikes().size()    : 0;
        long saves    = reel.getSavedBy()  != null ? reel.getSavedBy().size()  : 0;
        long comments = reel.getComments() != null ? reel.getComments().size() : 0;
        return likes + saves + (comments * COMMENT_WEIGHT);
    }

    private double recencyScore(LocalDateTime createdAt) {
        if (createdAt == null) return 0.0;
        long ageHours = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        double ageDays = ageHours / 24.0;
        return Math.pow(2.0, -ageDays / HALF_LIFE_DAYS);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Set<String> extractLabels(Reel reel) {
        Set<String> reelLabels = new HashSet<>();
        reelLabels.addAll(parsePreferences(reel.getPreferences()));

        ModerationResult mod = reel.getModerationResult();
        if (mod != null && mod.getAiContentLabels() != null && !mod.getAiContentLabels().isBlank()) {
            reelLabels.addAll(parsePreferences(mod.getAiContentLabels()));
        }
        return reelLabels;
    }

    private Set<String> parsePreferences(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    private boolean isPositive(ReelInteraction interaction) {
        InteractionType type = interaction.getInteractionType();
        if (type == InteractionType.COMPLETE || type == InteractionType.SHARE || type == InteractionType.COMMENT || type == InteractionType.LIKE || type == InteractionType.SAVE) {
            return true;
        }
        if (type == InteractionType.WATCH && interaction.getCompletionRate() != null && interaction.getCompletionRate() >= 0.8) {
            return true;
        }
        return false;
    }

    private boolean isNegative(ReelInteraction interaction) {
        InteractionType type = interaction.getInteractionType();
        if (type == InteractionType.NOT_INTERESTED) {
            return true;
        }
        if (type == InteractionType.SKIP && interaction.getWatchDurationMs() != null && interaction.getWatchDurationMs() < 2000) {
            return true;
        }
        if (type == InteractionType.WATCH && interaction.getCompletionRate() != null && interaction.getCompletionRate() < 0.2) {
            return true;
        }
        return false;
    }
}
