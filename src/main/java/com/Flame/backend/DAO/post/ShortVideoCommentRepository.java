package com.Flame.backend.DAO.post;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Flame.backend.entities.post.ShortVideoComment;

@Repository
public interface ShortVideoCommentRepository extends JpaRepository<ShortVideoComment, Long> {
    List<ShortVideoComment> findByVideoIdOrderByCreatedAtAsc(Long videoId);
    long countByVideoId(Long videoId);
}
