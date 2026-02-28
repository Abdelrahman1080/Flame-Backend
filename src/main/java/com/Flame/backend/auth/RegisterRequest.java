package com.Flame.backend.auth;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String companyName; // بس لل Provider
    private String role; // USER / PROVIDER
}
