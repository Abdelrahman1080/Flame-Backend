package com.Flame.backend.services.community;

import com.Flame.backend.DTO.community.*;
import com.Flame.backend.DTO.customer.UserResponse;
import com.Flame.backend.entities.community.Community;
import com.Flame.backend.entities.community.CommunityComment;
import com.Flame.backend.entities.community.CommunityPost;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.DAO.community.CommunityCommentRepository;
import com.Flame.backend.DAO.community.CommunityPostRepository;
import com.Flame.backend.DAO.community.CommunityRepository;
import com.Flame.backend.DAO.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository      communityRepository;
    private final CommunityPostRepository  communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final UserRepository           userRepository;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    // ── File upload ──────────────────────────────────────────────────────────

    public String savePhoto(MultipartFile file, String oldPhotoUrl) throws IOException {
        if (file == null || file.isEmpty()) return null;

        if (file.getSize() > MAX_FILE_SIZE)
            throw new RuntimeException("File too large. Max size is 10MB");

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.startsWith("image/"))
            throw new RuntimeException("Only image files are allowed");

        if (oldPhotoUrl != null)
            Files.deleteIfExists(Paths.get(oldPhotoUrl));

        String originalName = file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) extension = originalName.substring(dotIndex);

        Path uploadDir = Paths.get("uploads", "community-images").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/community-images/" + filename;
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getProfileUrl(),
                user.getRole()
        );
    }

    private CommunityCommentResponse toCommentResponse(CommunityComment comment) {
        return new CommunityCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                toUserResponse(comment.getAuthor())
        );
    }

    // currentUserId = null when called from a public context (no auth needed)
    private CommunityPostResponse toPostResponse(CommunityPost post, Integer currentUserId) {
        List<CommunityCommentResponse> comments = post.getComments().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        boolean likedByMe = currentUserId != null &&
                post.getLikes().stream().anyMatch(u -> u.getId().equals(currentUserId));

        return new CommunityPostResponse(
                post.getId(),
                post.getContent(),
                post.getImageUrl(),
                post.getCreatedAt(),
                toUserResponse(post.getAuthor()),
                post.getLikes().size(),
                likedByMe,
                comments
        );
    }

    private CommunityResponse toCommunityResponse(Community community) {
        return new CommunityResponse(
                community.getId(),
                community.getName(),
                community.getDescription(),
                community.getPhotoUrl(),
                community.getCreatedAt(),
                toUserResponse(community.getAdmin()),
                community.getMembers().size()
        );
    }

    private CommunityDetailResponse toCommunityDetailResponse(Community community, Integer currentUserId) {
        Set<UserResponse> members = community.getMembers().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toSet());

        List<CommunityPostResponse> posts = community.getPosts().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(p -> toPostResponse(p, currentUserId))
                .collect(Collectors.toList());

        return new CommunityDetailResponse(
                community.getId(),
                community.getName(),
                community.getDescription(),
                community.getPhotoUrl(),
                community.getCreatedAt(),
                toUserResponse(community.getAdmin()),
                members,
                posts
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Community getCommunityOrThrow(Integer communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));
    }

    private User getUserOrThrow(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private CommunityPost getPostOrThrow(Integer postId) {
        return communityPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    private void assertIsAdmin(Community community, Integer userId) {
        if (!community.getAdmin().getId().equals(userId))
            throw new RuntimeException("Only the community admin can perform this action.");
    }

    private void assertIsMemberOrAdmin(Community community, Integer userId) {
        boolean isAdmin  = community.getAdmin().getId().equals(userId);
        boolean isMember = community.getMembers().stream().anyMatch(m -> m.getId().equals(userId));
        if (!isAdmin && !isMember)
            throw new RuntimeException("You must be a member of this community.");
    }

    // ── Community CRUD ───────────────────────────────────────────────────────

    @Transactional
    public CommunityResponse createCommunity(User currentUser, CreateCommunityRequest request) {
        Community community = Community.builder()
                .name(request.getName())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .admin(currentUser)
                .build();
        return toCommunityResponse(communityRepository.save(community));
    }

    public CommunityDetailResponse getCommunity(Integer communityId, Integer currentUserId) {
        return toCommunityDetailResponse(getCommunityOrThrow(communityId), currentUserId);
    }

    public List<CommunityResponse> getAllCommunities() {
        return communityRepository.findAll().stream()
                .map(this::toCommunityResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommunityResponse updateCommunity(
            Integer currentUserId, Integer communityId,
            UpdateCommunityRequest request, MultipartFile photo
    ) throws IOException {
        Community community = getCommunityOrThrow(communityId);
        assertIsAdmin(community, currentUserId);

        if (request.getName() != null) community.setName(request.getName());
        if (photo != null && !photo.isEmpty()) {
            community.setPhotoUrl(savePhoto(photo, community.getPhotoUrl()));
        }
        return toCommunityResponse(communityRepository.save(community));
    }

    @Transactional
    public void deleteCommunity(Integer currentUserId, Integer communityId) {
        Community community = getCommunityOrThrow(communityId);
        assertIsAdmin(community, currentUserId);
        if (community.getPhotoUrl() != null) {
            try { Files.deleteIfExists(Paths.get(community.getPhotoUrl())); } catch (IOException ignored) {}
        }
        communityRepository.delete(community);
    }

    @Transactional
    public void removeMember(Integer currentUserId, Integer communityId, Integer memberId) {
        Community community = getCommunityOrThrow(communityId);
        assertIsAdmin(community, currentUserId);
        if (community.getAdmin().getId().equals(memberId))
            throw new RuntimeException("Cannot remove the admin from the community.");
        boolean removed = community.getMembers().removeIf(m -> m.getId().equals(memberId));
        if (!removed) throw new RuntimeException("User is not a member of this community.");
        communityRepository.save(community);
    }

    // ── Join / Leave ─────────────────────────────────────────────────────────

    @Transactional
    public void joinCommunity(Integer currentUserId, Integer communityId) {
        Community community = getCommunityOrThrow(communityId);
        if (community.getAdmin().getId().equals(currentUserId))
            throw new RuntimeException("You are already the admin of this community.");
        community.getMembers().add(getUserOrThrow(currentUserId));
        communityRepository.save(community);
    }

    @Transactional
    public void leaveCommunity(Integer currentUserId, Integer communityId) {
        Community community = getCommunityOrThrow(communityId);
        if (community.getAdmin().getId().equals(currentUserId))
            throw new RuntimeException("Admin cannot leave. Delete the community instead.");
        community.getMembers().removeIf(m -> m.getId().equals(currentUserId));
        communityRepository.save(community);
    }

    // ── Posts ────────────────────────────────────────────────────────────────

    @Transactional
    public CommunityPostResponse createPost(Integer currentUserId, Integer communityId, CreatePostRequest request) {
        Community community = getCommunityOrThrow(communityId);
        assertIsMemberOrAdmin(community, currentUserId);

        CommunityPost post = CommunityPost.builder()
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .author(getUserOrThrow(currentUserId))
                .community(community)
                .build();

        return toPostResponse(communityPostRepository.save(post), currentUserId);
    }

    public List<CommunityPostResponse> getPosts(Integer communityId, Integer currentUserId) {
        return getCommunityOrThrow(communityId).getPosts().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(p -> toPostResponse(p, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePost(Integer currentUserId, Integer communityId, Integer postId) {
        Community community = getCommunityOrThrow(communityId);
        CommunityPost post = getPostOrThrow(postId);

        boolean isAdmin  = community.getAdmin().getId().equals(currentUserId);
        boolean isAuthor = post.getAuthor().getId().equals(currentUserId);
        if (!isAdmin && !isAuthor)
            throw new RuntimeException("Only the post author or community admin can delete this post.");

        if (post.getImageUrl() != null) {
            try { Files.deleteIfExists(Paths.get(post.getImageUrl())); } catch (IOException ignored) {}
        }
        communityPostRepository.delete(post);
    }

    // ── Likes ────────────────────────────────────────────────────────────────

    @Transactional
    public CommunityPostResponse likePost(Integer currentUserId, Integer communityId, Integer postId) {
        Community community = getCommunityOrThrow(communityId);
        assertIsMemberOrAdmin(community, currentUserId);
        CommunityPost post = getPostOrThrow(postId);
        User user = getUserOrThrow(currentUserId);

        if (post.getLikes().stream().anyMatch(u -> u.getId().equals(currentUserId)))
            throw new RuntimeException("You already liked this post.");

        post.getLikes().add(user);
        return toPostResponse(communityPostRepository.save(post), currentUserId);
    }

    @Transactional
    public CommunityPostResponse unlikePost(Integer currentUserId, Integer communityId, Integer postId) {
        Community community = getCommunityOrThrow(communityId);
        assertIsMemberOrAdmin(community, currentUserId);
        CommunityPost post = getPostOrThrow(postId);

        post.getLikes().removeIf(u -> u.getId().equals(currentUserId));
        return toPostResponse(communityPostRepository.save(post), currentUserId);
    }

    // ── Comments ─────────────────────────────────────────────────────────────

    @Transactional
    public CommunityCommentResponse addComment(Integer currentUserId, Integer communityId, Integer postId, AddCommentRequest request) {
        Community community = getCommunityOrThrow(communityId);
        assertIsMemberOrAdmin(community, currentUserId);
        CommunityPost post = getPostOrThrow(postId);

        CommunityComment comment = CommunityComment.builder()
                .content(request.getContent())
                .author(getUserOrThrow(currentUserId))
                .post(post)
                .build();

        return toCommentResponse(communityCommentRepository.save(comment));
    }

    public List<CommunityCommentResponse> getComments(Integer communityId, Integer postId) {
        getCommunityOrThrow(communityId);
        return getPostOrThrow(postId).getComments().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(Integer currentUserId, Integer communityId, Integer postId, Integer commentId) {
        Community community = getCommunityOrThrow(communityId);
        CommunityComment comment = communityCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        boolean isAdmin  = community.getAdmin().getId().equals(currentUserId);
        boolean isAuthor = comment.getAuthor().getId().equals(currentUserId);
        if (!isAdmin && !isAuthor)
            throw new RuntimeException("Only the comment author or community admin can delete this comment.");

        communityCommentRepository.delete(comment);
    }

    // ── Members ──────────────────────────────────────────────────────────────

    public Set<UserResponse> getMembers(Integer communityId) {
        return getCommunityOrThrow(communityId).getMembers().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toSet());
    }
}