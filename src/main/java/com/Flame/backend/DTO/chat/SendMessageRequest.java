package com.Flame.backend.DTO.chat;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Integer receiverId;
    private String content;
}