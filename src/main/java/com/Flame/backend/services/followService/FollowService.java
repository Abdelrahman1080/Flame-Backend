package com.Flame.backend.services;

import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.DAO.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserRepository userRepository;

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getProfileUrl(),
                user.getRole()
        );
    }

    @Transactional
    public void follow(Integer currentUserId, Integer targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("A user cannot follow themselves.");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        targetUser.getFollowers().add(currentUser);
        userRepository.save(targetUser);
    }

    @Transactional
    public void unfollow(Integer currentUserId, Integer targetUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        targetUser.getFollowers().remove(currentUser);
        userRepository.save(targetUser);
    }

    public Set<UserResponse> getFollowers(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFollowers().stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }

    public Set<UserResponse> getFollowing(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFollowing().stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }
}