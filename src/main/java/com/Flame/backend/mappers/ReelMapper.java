package com.Flame.backend.mappers;

import com.Flame.backend.DTO.customer.CustomerLiteDTO;
import com.Flame.backend.DTO.reels.CommentDTO;
import com.Flame.backend.DTO.reels.ReelResponseDTO;
import com.Flame.backend.entities.Reels.Comment;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;

import java.util.stream.Collectors;

public class ReelMapper {

    public static ReelResponseDTO toDTO(Reel reel, Customer currentUser) {

        boolean liked = reel.getLikes().contains(currentUser);
        boolean saved = reel.getSavedBy().contains(currentUser);

        return ReelResponseDTO.builder()
                .id(reel.getId())
                .caption(reel.getCaption())
                .videoUrl(reel.getVideoUrl())
                .thumbnailUrl(reel.getThumbnailUrl())
                .durationSeconds(reel.getDurationSeconds())
                .likesCount((long) reel.getLikes().size())
                .commentsCount((long) reel.getComments().size())
                .likedByMe(liked)
                .savedByMe(saved)
                .creator(toCustomerLite(reel.getCreator()))
                .comments(
                        reel.getComments()
                                .stream()
                                .map(ReelMapper::toCommentDTO)
                                .collect(Collectors.toList())
                )
                // FIX: was silently dropped — createdAt exists on both entity and DTO
                .createdAt(reel.getCreatedAt())
                .build();
    }

    public static CommentDTO toCommentDTO(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(toCustomerLite(comment.getUser()))
                .build();
    }

    /**
     * FIX: added null-guard — a reel with no creator would previously throw NPE.
     * Null creator returns null CustomerLiteDTO (handled gracefully by the frontend).
     */
    public static CustomerLiteDTO toCustomerLite(Customer c) {
        if (c == null) return null;
        return CustomerLiteDTO.builder()
                .id(c.getId())
                .name(c.getFirstname())
                .build();
    }
}