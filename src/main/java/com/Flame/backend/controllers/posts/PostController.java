package com.Flame.backend.controllers.posts;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.DAO.post.PostCommentRepository;
import com.Flame.backend.DAO.post.PostLikeRepository;
import com.Flame.backend.DAO.post.PostRepository;
import com.Flame.backend.entities.post.Post;
import com.Flame.backend.entities.post.PostComment;
import com.Flame.backend.entities.post.PostLike;
import com.Flame.backend.entities.user.Role;
import com.Flame.backend.entities.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostLikeRepository postLikeRepository;

    public record AuthorSummary(Integer id, String firstName, String lastName, String email) {
    }

    public record CommentRequest(String content) {
    }

    public record PostRequest(String content) {
    }

    public record CommentResponse(Long id, String content, AuthorSummary author, LocalDateTime createdAt) {
    }

    public record PostResponse(
            Long id,
            String content,
            AuthorSummary author,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long likeCount,
            long commentCount,
            boolean likedByMe,
            List<CommentResponse> comments
    ) {
    }

    public record LikeToggleResponse(Long postId, boolean liked, long likeCount) {
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }

    private Post getPostOrThrow(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (post.isSuspended()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        return post;
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }
        return normalized;
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private void assertOwnerOrAdmin(User currentUser, Integer ownerId) {
        if (ownerId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action is not allowed");
        }

        if (isAdmin(currentUser)) {
            return;
        }

        if (!ownerId.equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action is not allowed");
        }
    }

    private AuthorSummary toAuthorSummary(User user) {
        return new AuthorSummary(user.getId(), user.getFirstname(), user.getLastname(), user.getEmail());
    }

    private CommentResponse toCommentResponse(PostComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                toAuthorSummary(comment.getAuthor()),
                comment.getCreatedAt()
        );
    }

    private PostResponse toPostResponse(Post post, User currentUser, boolean includeComments) {
        long likeCount = postLikeRepository.countByPostId(post.getId());
        long commentCount = postCommentRepository.countByPostId(post.getId());
        boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());

        List<CommentResponse> comments = includeComments
                ? postCommentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).stream()
                        .map(this::toCommentResponse)
                        .toList()
                : List.of();

        return new PostResponse(
                post.getId(),
                post.getContent(),
                toAuthorSummary(post.getAuthor()),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                likeCount,
                commentCount,
                likedByMe,
                comments
        );
    }

    @GetMapping
    public List<PostResponse> getPosts(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return postRepository.findAllBySuspendedFalseOrderByCreatedAtDesc().stream()
                .map(post -> toPostResponse(post, currentUser, false))
                .toList();
    }

    @GetMapping("/{postId}")
    public PostResponse getPostById(@PathVariable Long postId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Post post = getPostOrThrow(postId);
        return toPostResponse(post, currentUser, true);
    }

    @PostMapping
    @Transactional
    public PostResponse createPost(@RequestBody PostRequest request, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);

        Post post = new Post();
        post.setContent(normalizeContent(request == null ? null : request.content()));
        post.setAuthor(currentUser);

        Post saved = postRepository.save(post);
        return toPostResponse(saved, currentUser, false);
    }

    @PutMapping("/{postId}")
    @Transactional
    public PostResponse updatePost(
            @PathVariable Long postId,
            @RequestBody PostRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Post post = getPostOrThrow(postId);

        assertOwnerOrAdmin(currentUser, post.getAuthor().getId());

        post.setContent(normalizeContent(request == null ? null : request.content()));
        Post saved = postRepository.save(post);

        return toPostResponse(saved, currentUser, false);
    }

    @DeleteMapping("/{postId}")
    @Transactional
    public Map<String, String> deletePost(@PathVariable Long postId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Post post = getPostOrThrow(postId);

        assertOwnerOrAdmin(currentUser, post.getAuthor().getId());

        postRepository.delete(post);
        return Map.of("message", "Post deleted successfully");
    }

    @GetMapping("/{postId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long postId, Authentication authentication) {
        getCurrentUser(authentication);
        getPostOrThrow(postId);
        return postCommentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @PostMapping("/{postId}/comments")
    @Transactional
    public CommentResponse addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Post post = getPostOrThrow(postId);

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setAuthor(currentUser);
        comment.setContent(normalizeContent(request == null ? null : request.content()));

        PostComment saved = postCommentRepository.save(comment);
        return toCommentResponse(saved);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @Transactional
    public Map<String, String> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Post post = getPostOrThrow(postId);

        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!comment.getPost().getId().equals(post.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment does not belong to this post");
        }

        boolean isCommentAuthor = comment.getAuthor().getId().equals(currentUser.getId());
        boolean isPostAuthor = post.getAuthor().getId().equals(currentUser.getId());

        if (!isAdmin(currentUser) && !isCommentAuthor && !isPostAuthor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Action is not allowed");
        }

        postCommentRepository.delete(comment);
        return Map.of("message", "Comment deleted successfully");
    }

    @PostMapping("/{postId}/likes/toggle")
    @Transactional
    public LikeToggleResponse toggleLike(@PathVariable Long postId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        Post post = getPostOrThrow(postId);

        PostLike existingLike = postLikeRepository.findByPostIdAndUserId(postId, currentUser.getId()).orElse(null);

        boolean liked;
        if (existingLike == null) {
            PostLike newLike = new PostLike();
            newLike.setPost(post);
            newLike.setUser(currentUser);
            postLikeRepository.save(newLike);
            liked = true;
        } else {
            postLikeRepository.delete(existingLike);
            liked = false;
        }

        long likeCount = postLikeRepository.countByPostId(postId);
        return new LikeToggleResponse(postId, liked, likeCount);
    }

    @GetMapping("/{postId}/likes")
    public Map<String, Object> getLikeSummary(@PathVariable Long postId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        getPostOrThrow(postId);

        long likeCount = postLikeRepository.countByPostId(postId);
        boolean likedByMe = postLikeRepository.existsByPostIdAndUserId(postId, currentUser.getId());

        return Map.of(
                "postId", postId,
                "likeCount", likeCount,
                "likedByMe", likedByMe
        );
    }
}
