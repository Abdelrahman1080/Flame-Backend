package com.Flame.backend.controllers.followers;

import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.services.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    // POST /api/users/{targetId}/follow
    @PostMapping("/{targetId}/follow")
    public ResponseEntity<String> follow(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer targetId
    ) {
        followService.follow(currentUser.getId(), targetId);
        return ResponseEntity.ok("Followed successfully.");
    }

    // DELETE /api/users/{targetId}/follow
    @DeleteMapping("/{targetId}/follow")
    public ResponseEntity<String> unfollow(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer targetId
    ) {
        followService.unfollow(currentUser.getId(), targetId);
        return ResponseEntity.ok("Unfollowed successfully.");
    }

    // GET /api/users/{userId}/followers
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Set<UserResponse>> getFollowers(@PathVariable Integer userId) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    // GET /api/users/{userId}/following
    @GetMapping("/{userId}/following")
    public ResponseEntity<Set<UserResponse>> getFollowing(@PathVariable Integer userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
    }
}