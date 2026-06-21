package com.Flame.backend.services.reels;

import com.Flame.backend.DAO.reels.CommentRepository;
import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DTO.reels.CommentDTO;
import com.Flame.backend.DTO.reels.CreateCommentRequestDTO;
import com.Flame.backend.entities.Reels.Comment;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.mappers.ReelMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final ReelRepository reelRepository;
    private final CommentRepository commentRepository;
    private final CustomerRepository customerRepository;

    // ---------------- ADD COMMENT ----------------

    @Override
    public CommentDTO addComment(Long reelId, CreateCommentRequestDTO dto) {

        Customer customer = getCurrentUser();

        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        Comment comment = Comment.builder()
                .content(dto.getContent())
                .user(customer)
                .reel(reel)
                .build();

        Comment saved = commentRepository.save(comment);

        return ReelMapper.toCommentDTO(saved);
    }

    // ---------------- GET COMMENTS ----------------

    @Override
    public List<CommentDTO> getComments(Long reelId) {

        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        return commentRepository.findByReel(reel)
                .stream()
                .map(ReelMapper::toCommentDTO)
                .toList();
    }


    // ---------------- AUTH ----------------

    private Customer getCurrentUser() {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        String email = auth.getName();

        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }
}