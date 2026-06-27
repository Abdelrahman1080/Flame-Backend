package com.Flame.backend.DTO.community;

import com.Flame.backend.DTO.customer.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommunityCommentResponse {
    private Integer id;
    private String content;
    private LocalDateTime createdAt;
    private UserResponse author;
}