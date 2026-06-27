package com.Flame.backend.DTO.chat;

import com.Flame.backend.DTO.customer.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Integer id;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;
    private UserResponse sender;
    private UserResponse receiver;
}