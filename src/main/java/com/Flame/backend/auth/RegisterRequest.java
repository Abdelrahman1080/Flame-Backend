package com.Flame.backend.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String companyLogoUrl;
    private String companyTagline;
    private String companyDescription;
    private String companyWebsite;
    private String companyLocation;
    private String role; // USER / PROVIDER
    private String preferences; // comma-separated preferences for recommendations
}
