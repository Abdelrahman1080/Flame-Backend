package com.Flame.backend.DTO.customer;

import com.Flame.backend.entities.user.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

    private Integer id;
    private String firstname;
    private String lastname;
    private String email;
    private String profileUrl;
    private Role role;
}
