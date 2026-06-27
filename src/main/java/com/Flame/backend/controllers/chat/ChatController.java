package com.Flame.backend.controllers;

import com.Flame.backend.DTO.chat.ChatMessageResponse;
import com.Flame.backend.DTO.chat.InboxResponse;
import com.Flame.backend.DTO.chat.SendMessageRequest;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.services.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // POST /api/chat/send — send a message
    @PostMapping("/send")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            Authentication authentication,
            @RequestBody SendMessageRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendMessage(currentUser.getId(), request));
    }

    // GET /api/chat/inbox — get all conversations
    @GetMapping("/inbox")
    public ResponseEntity<List<InboxResponse>> getInbox(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(chatService.getInbox(currentUser.getId()));
    }

    // GET /api/chat/{userId} — get conversation with a specific user
    @GetMapping("/{userId}")
    public ResponseEntity<List<ChatMessageResponse>> getConversation(
            Authentication authentication,
            @PathVariable Integer userId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(chatService.getConversation(currentUser.getId(), userId));
    }
}