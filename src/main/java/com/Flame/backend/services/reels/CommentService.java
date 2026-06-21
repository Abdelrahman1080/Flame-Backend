package com.Flame.backend.services.reels;

import com.Flame.backend.DTO.reels.CommentDTO;
import com.Flame.backend.DTO.reels.CreateCommentRequestDTO;
import com.Flame.backend.entities.Reels.Comment;

import java.util.List;

public interface CommentService {

    CommentDTO addComment(Long reelId, CreateCommentRequestDTO comment);

    List<CommentDTO> getComments(Long reelId);


}