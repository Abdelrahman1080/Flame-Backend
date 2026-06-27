package com.Flame.backend.DTO.community;

import com.Flame.backend.DTO.customer.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommunityResponse {
    private Integer id;
    private String name;
    private String description;
    private String photoUrl;
    private LocalDateTime createdAt;
    private UserResponse admin;
    private int memberCount;
}
