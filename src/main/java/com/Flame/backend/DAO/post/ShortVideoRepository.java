package com.Flame.backend.DAO.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Flame.backend.entities.post.ShortVideo;

@Repository
public interface ShortVideoRepository extends JpaRepository<ShortVideo, Long> {
    Page<ShortVideo> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
