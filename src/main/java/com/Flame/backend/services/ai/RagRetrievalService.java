package com.Flame.backend.services.ai;

import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DTO.ai.RagSourceDTO;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.ReelStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagRetrievalService {

    private final ReelRepository reelRepository;

    public record RetrievalResult(String contextText, List<RagSourceDTO> sources) {}

    /**
     * Extracts keywords and finds relevant APPROVED reels to use as RAG context.
     */
    public RetrievalResult retrieveContext(String userMessage, Customer customer) {
        // Very basic keyword extraction (MVP)
        List<String> keywords = extractKeywords(userMessage);
        
        // If no keywords found in message, fallback to user's preferences if available
        if (keywords.isEmpty() && customer != null && customer.getPreferences() != null) {
            keywords.addAll(Arrays.asList(customer.getPreferences().split(",")));
        }

        List<Reel> relevantReels = new ArrayList<>();
        
        // Fetch matching APPROVED reels
        for (String keyword : keywords) {
            if (keyword.isBlank()) continue;
            List<Reel> found = reelRepository.findTop5ForRagContext(
                    ReelStatus.APPROVED, 
                    keyword.trim(), 
                    PageRequest.of(0, 3) // Top 3 per keyword to avoid context overflow
            );
            relevantReels.addAll(found);
        }

        // Deduplicate
        List<Reel> uniqueReels = relevantReels.stream().distinct().limit(5).toList();

        // Build context string and DTOs
        StringBuilder contextBuilder = new StringBuilder();
        List<RagSourceDTO> sources = new ArrayList<>();

        if (customer != null && customer.getPreferences() != null) {
            contextBuilder.append("[User Context]\n")
                          .append("Preferences: ").append(customer.getPreferences()).append("\n\n");
        }

        for (Reel reel : uniqueReels) {
            String safeCaption = sanitizeContext(reel.getCaption());
            String safePrefs = sanitizeContext(reel.getPreferences());

            contextBuilder.append("[Reel #").append(reel.getId()).append("]\n")
                          .append("Caption: ").append(safeCaption).append("\n")
                          .append("Tags/Preferences: ").append(safePrefs).append("\n")
                          .append("CreatedAt: ").append(reel.getCreatedAt()).append("\n\n");

            sources.add(RagSourceDTO.builder()
                    .type("reel")
                    .id(reel.getId())
                    .title(safeCaption.length() > 50 ? safeCaption.substring(0, 47) + "..." : safeCaption)
                    .reason("Matched keywords or user preferences")
                    .build());
        }

        return new RetrievalResult(contextBuilder.toString().trim(), sources);
    }

    /**
     * Strips potentially dangerous instruction syntax from raw database strings 
     * before injecting them into the LLM prompt.
     */
    private String sanitizeContext(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)(<system>|<instruction>|\\[instruction\\]|\\{\\{|\\}\\})", "[REDACTED]");
    }

    private List<String> extractKeywords(String message) {
        // Basic stop-word removal MVP
        String[] words = message.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        List<String> stopWords = List.of("what", "is", "are", "the", "a", "an", "for", "me", "show", "some", "i", "want", "to", "see", "find", "my");
        return Arrays.stream(words)
                .filter(w -> !stopWords.contains(w) && w.length() > 2)
                .collect(Collectors.toList());
    }
}
