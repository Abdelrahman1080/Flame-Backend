package com.Flame.backend.DTO.community;

import com.Flame.backend.DTO.customer.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommunityPostResponse {
    private Integer id;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private UserResponse author;
    private int likesCount;
    private boolean likedByMe;
    private List<CommunityCommentResponse> comments;
}