package com.Flame.backend.controllers.ai;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.entities.user.User;
import com.Flame.backend.services.ai.AIChatService;
import com.Flame.backend.services.ai.AIChatService.MessageHistory;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;

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
}
