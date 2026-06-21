package com.Flame.backend.controllers.reels;

import com.Flame.backend.DTO.reels.CommentDTO;
import com.Flame.backend.DTO.reels.CreateCommentRequestDTO;
import com.Flame.backend.entities.Reels.Comment;
import com.Flame.backend.services.reels.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{reelId}")
    public CommentDTO addComment(
            @PathVariable Long reelId,
            @RequestBody CreateCommentRequestDTO comment) {

        return commentService.addComment(reelId, comment);
    }

    @GetMapping("/{reelId}")
    public List<CommentDTO> getComments(@PathVariable Long reelId) {
        return commentService.getComments(reelId);
    }


}