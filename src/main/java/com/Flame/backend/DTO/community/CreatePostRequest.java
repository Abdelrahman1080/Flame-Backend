package com.Flame.backend.DTO.community;

import lombok.Data;

@Data
public class CreatePostRequest {
    private String content;
    private String imageUrl;
}
