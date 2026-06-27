package com.Flame.backend.DTO.community;

import lombok.Data;

@Data
public class CreateCommunityRequest {
    private String name;
    private String description;
    private String photoUrl;
}
