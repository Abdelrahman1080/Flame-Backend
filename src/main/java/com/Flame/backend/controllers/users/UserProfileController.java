package com.Flame.backend.controllers.users;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.Role;
import com.Flame.backend.services.reelsModeration.GcsFileUploadService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.entities.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final UserRepository userRepository;
    private final GcsFileUploadService gcsFileUploadService;

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        Role role = (user instanceof Customer)
                ? Role.USER
                : Role.ADMIN;

        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getProfileUrl(),
                role
        );
    }
/*
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public User uploadProfileImage(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No file uploaded");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File too large. Max size is 10MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        String originalName = file.getOriginalFilename() == null ? "profile" : file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        Path uploadDir = Paths.get("uploads", "profile-images").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        User user = (User) authentication.getPrincipal();
        if(user != null) {
            if (user.getProfileUrl() != null) {
                Files.deleteIfExists(Paths.get(user.getProfileUrl()));
            }
            user.setProfileUrl("/uploads/profile-images/" + filename);
            return userRepository.save(user);
        }
        return null;
    }*/


    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public User uploadProfileImage(@RequestParam("file") MultipartFile file,
                                   Authentication authentication) throws IOException {
        if (file == null || file.isEmpty())
            throw new RuntimeException("No file uploaded");

        User user = (User) authentication.getPrincipal();

        // Delete old profile image from GCS
        if (user.getProfileUrl() != null) {
            gcsFileUploadService.deleteFile(user.getProfileUrl());
        }

        // Upload new image to GCS
        String newUrl = gcsFileUploadService.uploadImage(file, "profile-images");
        user.setProfileUrl(newUrl);

        return userRepository.save(user);
    }
}
