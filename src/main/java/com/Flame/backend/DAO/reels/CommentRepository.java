package com.Flame.backend.DAO.reels;

import com.Flame.backend.entities.Reels.Comment;
import com.Flame.backend.entities.Reels.Reel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByReel(Reel reel);

}