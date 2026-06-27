package com.Flame.backend.controllers.ai;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.services.ai.AIChatService;
import com.Flame.backend.services.ai.AIChatService.MessageHistory;
import com.Flame.backend.services.ai.RagRetrievalService.RetrievalResult;
import com.Flame.backend.DTO.ai.RagChatRequest;
import com.Flame.backend.DTO.ai.RagChatResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIChatController {

    private final AIChatService aiChatService;

    // Simple in-memory rate limiter: CustomerId -> (RequestCount, WindowStartTimestamp)
    private final Map<Long, RateLimitData> rateLimits = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_MS = 10 * 60 * 1000; // 10 minutes

    private static class RateLimitData {
        int count;
        long windowStart;
        RateLimitData(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }

    private void enforceRateLimit(Long customerId) {
        if (customerId == null) return; // Skip rate limit if no specific customer id
        long now = Instant.now().toEpochMilli();
        rateLimits.compute(customerId, (id, data) -> {
            if (data == null || now - data.windowStart > WINDOW_MS) {
                return new RateLimitData(1, now);
            }
            if (data.count >= MAX_REQUESTS) {
                log.warn("Rate limit exceeded for customer {}", customerId);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait before trying again.");
            }
            data.count++;
            return data;
        });
    }

    public record AIChatRequest(String message, List<MessageHistory> context) {}
    public record AIChatResponse(String reply) {}

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }

    @PostMapping("/chat")
    public AIChatResponse chat(@RequestBody AIChatRequest request, Authentication authentication) {
        // Enforce authentication so only logged-in users can use the AI
        getCurrentUser(authentication);

        if (request.message() == null || request.message().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty");
        }

        String aiReply = aiChatService.generateResponse(request.message(), request.context());
        return new AIChatResponse(aiReply);
    }

    @PostMapping("/rag-chat")
    public RagChatResponse ragChat(@RequestBody RagChatRequest request, Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Customer customer = (user instanceof Customer) ? (Customer) user : null;

            if (customer != null && customer.getId() != null) {
                enforceRateLimit(((Number) customer.getId()).longValue());
            }

            String msg = request.getMessage();
            if (msg == null || msg.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty.");
            }
            
            msg = msg.trim();
            if (msg.length() > 1000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message too long. Maximum 1000 characters allowed.");
            }

            String convId = request.getConversationId();
            if (convId != null && convId.length() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ConversationId too long.");
            }

            RetrievalResult result = aiChatService.generateRagResponse(msg, customer);

            return RagChatResponse.builder()
                    .answer(result.contextText())
                    .sources(result.sources())
                    .conversationId(convId != null ? convId : java.util.UUID.randomUUID().toString())
                    .build();
        } catch (ResponseStatusException rse) {
            throw rse; // Re-throw known, safe HTTP exceptions
        } catch (Exception e) {
            log.error("Internal AI Chat error processing request", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI service is currently unavailable.");
        }
    }
}
