package com.Flame.backend.DTO.community;

import com.Flame.backend.DTO.customer.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommunityDetailResponse {
    private Integer id;
    private String name;
    private String description;
    private String photoUrl;
    private LocalDateTime createdAt;
    private UserResponse admin;
    private Set<UserResponse> members;
    private List<CommunityPostResponse> posts;
}
