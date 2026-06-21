package com.Flame.backend.controllers.posts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.DAO.post.ShortVideoCommentRepository;
import com.Flame.backend.DAO.post.ShortVideoLikeRepository;
import com.Flame.backend.DAO.post.ShortVideoRepository;
import com.Flame.backend.entities.post.ShortVideo;
import com.Flame.backend.entities.post.ShortVideoComment;
import com.Flame.backend.entities.post.ShortVideoLike;
import com.Flame.backend.entities.user.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shorts")
@RequiredArgsConstructor
public class ShortVideoController {

    private final ShortVideoRepository shortVideoRepository;
    private final ShortVideoLikeRepository shortVideoLikeRepository;
    private final ShortVideoCommentRepository shortVideoCommentRepository;

    private final Path fileStorageLocation = Paths.get("uploads", "shorts").toAbsolutePath().normalize();

    public record AuthorSummary(Integer id, String firstName, String lastName, String email) {}

    public record CommentRequest(String content) {}

    public record CommentResponse(Long id, String content, AuthorSummary author, LocalDateTime createdAt) {}

    public record ShortVideoResponse(
            Long id,
            String title,
            String description,
            String videoUrl,
            AuthorSummary author,
            LocalDateTime createdAt,
            long likeCount,
            long commentCount,
            boolean likedByMe
    ) {}

    public record LikeToggleResponse(Long videoId, boolean liked, long likeCount) {}

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }

    private AuthorSummary toAuthorSummary(User user) {
        return new AuthorSummary(user.getId(), user.getFirstname(), user.getLastname(), user.getEmail());
    }

    private CommentResponse toCommentResponse(ShortVideoComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                toAuthorSummary(comment.getAuthor()),
                comment.getCreatedAt()
        );
    }

    private ShortVideoResponse toResponse(ShortVideo video, User currentUser) {
        long likeCount = shortVideoLikeRepository.countByVideoId(video.getId());
        long commentCount = shortVideoCommentRepository.countByVideoId(video.getId());
        boolean likedByMe = currentUser != null && shortVideoLikeRepository.existsByVideoIdAndUserId(video.getId(), currentUser.getId());

        return new ShortVideoResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getVideoUrl(),
                toAuthorSummary(video.getAuthor()),
                video.getCreatedAt(),
                likeCount,
                commentCount,
                likedByMe
        );
    }

    private ShortVideo getVideoOrThrow(Long videoId) {
        return shortVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short video not found"));
    }

    @PostMapping("/upload")
    @Transactional
    public ShortVideoResponse uploadShortVideo(
            @RequestParam("video") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);

        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            Files.createDirectories(this.fileStorageLocation);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileDownloadUri = "/uploads/shorts/" + uniqueFilename;

            ShortVideo video = new ShortVideo();
            video.setTitle(title);
            video.setDescription(description);
            video.setVideoUrl(fileDownloadUri);
            video.setAuthor(currentUser);

            ShortVideo savedVideo = shortVideoRepository.save(video);
            return toResponse(savedVideo, currentUser);

        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file", ex);
        }
    }

    @GetMapping
    public Page<ShortVideoResponse> getShortVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        User currentUser = null;
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            currentUser = user;
        }
        
        final User finalUser = currentUser;
        Pageable pageable = PageRequest.of(page, size);
        return shortVideoRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(video -> toResponse(video, finalUser));
    }

    @PostMapping("/{videoId}/likes/toggle")
    @Transactional
    public LikeToggleResponse toggleLike(@PathVariable Long videoId, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        ShortVideo video = getVideoOrThrow(videoId);

        ShortVideoLike existingLike = shortVideoLikeRepository.findByVideoIdAndUserId(videoId, currentUser.getId()).orElse(null);

        boolean liked;
        if (existingLike == null) {
            ShortVideoLike newLike = new ShortVideoLike();
            newLike.setVideo(video);
            newLike.setUser(currentUser);
            shortVideoLikeRepository.save(newLike);
            liked = true;
        } else {
            shortVideoLikeRepository.delete(existingLike);
            liked = false;
        }

        long likeCount = shortVideoLikeRepository.countByVideoId(videoId);
        return new LikeToggleResponse(videoId, liked, likeCount);
    }

    @GetMapping("/{videoId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long videoId) {
        getVideoOrThrow(videoId);
        return shortVideoCommentRepository.findByVideoIdOrderByCreatedAtAsc(videoId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @PostMapping("/{videoId}/comments")
    @Transactional
    public CommentResponse addComment(
            @PathVariable Long videoId,
            @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        ShortVideo video = getVideoOrThrow(videoId);

        if (request == null || request.content() == null || request.content().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content is required");
        }

        ShortVideoComment comment = new ShortVideoComment();
        comment.setVideo(video);
        comment.setAuthor(currentUser);
        comment.setContent(request.content().trim());

        ShortVideoComment saved = shortVideoCommentRepository.save(comment);
        return toCommentResponse(saved);
    }
}
