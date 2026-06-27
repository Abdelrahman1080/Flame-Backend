package com.Flame.backend.services.chat;

import com.Flame.backend.DAO.chat.ChatMessageRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.DTO.chat.ChatMessageResponse;
import com.Flame.backend.DTO.chat.InboxResponse;
import com.Flame.backend.DTO.chat.SendMessageRequest;
import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.entities.chat.ChatMessage;
import com.Flame.backend.entities.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // ── Mapper ───────────────────────────────────────────────────────────────

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getProfileUrl(),
                user.getRole()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getContent(),
                message.getSentAt(),
                message.isRead(),
                toUserResponse(message.getSender()),
                toUserResponse(message.getReceiver())
        );
    }

    // ── Send Message ─────────────────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse sendMessage(Integer senderId, SendMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (senderId.equals(request.getReceiverId()))
            throw new RuntimeException("You cannot send a message to yourself.");

        ChatMessage message = ChatMessage.builder()
                .content(request.getContent())
                .sender(sender)
                .receiver(receiver)
                .build();

        return toMessageResponse(chatMessageRepository.save(message));
    }

    // ── Get Conversation ─────────────────────────────────────────────────────

    @Transactional
    public List<ChatMessageResponse> getConversation(Integer currentUserId, Integer otherUserId) {
        List<ChatMessage> messages = chatMessageRepository.findConversation(currentUserId, otherUserId);

        // Mark all received messages as read
        messages.forEach(m -> {
            if (m.getReceiver().getId().equals(currentUserId) && !m.isRead()) {
                m.setRead(true);
            }
        });
        chatMessageRepository.saveAll(messages);

        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    // ── Get Inbox ─────────────────────────────────────────────────────────────

    public List<InboxResponse> getInbox(Integer currentUserId) {
        return chatMessageRepository.findInbox(currentUserId).stream()
                .map(m -> {
                    User otherUser = m.getSender().getId().equals(currentUserId)
                            ? m.getReceiver()
                            : m.getSender();

                    long unreadCount = chatMessageRepository
                            .countByReceiverIdAndIsReadFalse(currentUserId);

                    return new InboxResponse(
                            toUserResponse(otherUser),
                            m.getContent(),
                            m.getSentAt(),
                            m.isRead(),
                            unreadCount
                    );
                })
                .collect(Collectors.toList());
    }
}