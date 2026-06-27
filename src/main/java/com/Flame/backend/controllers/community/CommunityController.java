package com.Flame.backend.controllers.community;

import com.Flame.backend.DTO.community.*;
import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.services.community.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    // ── Community CRUD ───────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityResponse> createCommunity(
            Authentication authentication,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) throws IOException {
        User currentUser = (User) authentication.getPrincipal();
        String photoUrl = communityService.savePhoto(photo, null);
        CreateCommunityRequest request = new CreateCommunityRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPhotoUrl(photoUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createCommunity(currentUser, request));
    }

    @GetMapping
    public ResponseEntity<List<CommunityResponse>> getAllCommunities() {
        return ResponseEntity.ok(communityService.getAllCommunities());
    }

    @GetMapping("/{communityId}")
    public ResponseEntity<CommunityDetailResponse> getCommunity(
            Authentication authentication,
            @PathVariable Integer communityId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(communityService.getCommunity(communityId, currentUser.getId()));
    }

    // Admin only
    @PatchMapping(value = "/{communityId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityResponse> updateCommunity(
            Authentication authentication,
            @PathVariable Integer communityId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) throws IOException {
        User currentUser = (User) authentication.getPrincipal();
        UpdateCommunityRequest request = new UpdateCommunityRequest();
        request.setName(name);
        return ResponseEntity.ok(
                communityService.updateCommunity(currentUser.getId(), communityId, request, photo)
        );
    }

    // Admin only
    @DeleteMapping("/{communityId}")
    public ResponseEntity<String> deleteCommunity(
            Authentication authentication,
            @PathVariable Integer communityId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        communityService.deleteCommunity(currentUser.getId(), communityId);
        return ResponseEntity.ok("Community deleted.");
    }

    // ── Membership ───────────────────────────────────────────────────────────

    @PostMapping("/{communityId}/join")
    public ResponseEntity<String> joinCommunity(
            Authentication authentication,
            @PathVariable Integer communityId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        communityService.joinCommunity(currentUser.getId(), communityId);
        return ResponseEntity.ok("Joined community.");
    }

    @DeleteMapping("/{communityId}/leave")
    public ResponseEntity<String> leaveCommunity(
            Authentication authentication,
            @PathVariable Integer communityId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        communityService.leaveCommunity(currentUser.getId(), communityId);
        return ResponseEntity.ok("Left community.");
    }

    @GetMapping("/{communityId}/members")
    public ResponseEntity<Set<UserResponse>> getMembers(@PathVariable Integer communityId) {
        return ResponseEntity.ok(communityService.getMembers(communityId));
    }

    // Admin only
    @DeleteMapping("/{communityId}/members/{memberId}")
    public ResponseEntity<String> removeMember(
            Authentication authentication,
            @PathVariable Integer communityId,
            @PathVariable Integer memberId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        communityService.removeMember(currentUser.getId(), communityId, memberId);
        return ResponseEntity.ok("Member removed.");
    }

    // ── Posts ────────────────────────────────────────────────────────────────

    @PostMapping(value = "/{communityId}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityPostResponse> createPost(
            Authentication authentication,
            @PathVariable Integer communityId,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) throws IOException {
        User currentUser = (User) authentication.getPrincipal();
        String imageUrl = communityService.savePhoto(image, null);
        CreatePostRequest request = new CreatePostRequest();
        request.setContent(content);
        request.setImageUrl(imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createPost(currentUser.getId(), communityId, request));
    }

    @GetMapping("/{communityId}/posts")
    public ResponseEntity<List<CommunityPostResponse>> getPosts(
            Authentication authentication,
            @PathVariable Integer communityId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(communityService.getPosts(communityId, currentUser.getId()));
    }

    // Admin or post author
    @DeleteMapping("/{communityId}/posts/{postId}")
    public ResponseEntity<String> deletePost(
            Authentication authentication,
            @PathVariable Integer communityId,
            @PathVariable Integer postId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        communityService.deletePost(currentUser.getId(), communityId, postId);
        return ResponseEntity.ok("Post deleted.");
    }

    // ── Likes ────────────────────────────────────────────────────────────────

    @PostMapping("/{communityId}/posts/{postId}/like")
    public ResponseEntity<CommunityPostResponse> likePost(
            Authentication authentication,
            @PathVariable Integer communityId,
            @PathVariable Integer postId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(communityService.likePost(currentUser.getId(), communityId, postId));
    }

    @DeleteMapping("/{communityId}/posts/{postId}/like")
    public ResponseEntity<CommunityPostResponse> unlikePost(
            Authentication authentication,
            @PathVariable Integer communityId,
            @PathVariable Integer postId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(communityService.unlikePost(currentUser.getId(), communityId, postId));
    }

    // ── Comments ─────────────────────────────────────────────────────────────

    @PostMapping("/{communityId}/posts/{postId}/comments")
    public ResponseEntity<CommunityCommentResponse> addComment(
            Authentication authentication,
            @PathVariable Integer communityId,
            @PathVariable Integer postId,
            @RequestBody AddCommentRequest request
    ) {
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.addComment(currentUser.getId(), communityId, postId, request));
    }

    @GetMapping("/{communityId}/posts/{postId}/comments")
    public ResponseEntity<List<CommunityCommentResponse>> getComments(
            @PathVariable Integer communityId,
            @PathVariable Integer postId
    ) {
        return ResponseEntity.ok(communityService.getComments(communityId, postId));
    }

    // Admin or comment author
    @DeleteMapping("/{communityId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(
            Authentication authentication,
            @PathVariable Integer communityId,
            @PathVariable Integer postId,
            @PathVariable Integer commentId
    ) {
        User currentUser = (User) authentication.getPrincipal();
        communityService.deleteComment(currentUser.getId(), communityId, postId, commentId);
        return ResponseEntity.ok("Comment deleted.");
    }
}