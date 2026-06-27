package com.Flame.backend.DTO.chat;

import com.Flame.backend.DTO.customer.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InboxResponse {
    private UserResponse otherUser;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private boolean isRead;
    private long unreadCount;
}