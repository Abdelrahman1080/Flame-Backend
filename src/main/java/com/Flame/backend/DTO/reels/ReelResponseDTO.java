package com.Flame.backend.DTO.reels;

import com.Flame.backend.DTO.customer.CustomerLiteDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReelResponseDTO {

    private Long id;

    private String caption;

    private String videoUrl;

    private String thumbnailUrl;

    private Integer durationSeconds;

    private Long views;

    private Long likesCount;

    private Long commentsCount;

    private boolean likedByMe;

    private boolean savedByMe;

    private CustomerLiteDTO creator;

    private List<CommentDTO> comments;

    private LocalDateTime createdAt;
}
