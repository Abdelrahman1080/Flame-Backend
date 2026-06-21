package com.Flame.backend.DAO.post;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Flame.backend.entities.post.ShortVideoLike;

@Repository
public interface ShortVideoLikeRepository extends JpaRepository<ShortVideoLike, Long> {
    long countByVideoId(Long videoId);
    boolean existsByVideoIdAndUserId(Long videoId, Integer userId);
    Optional<ShortVideoLike> findByVideoIdAndUserId(Long videoId, Integer userId);
}
