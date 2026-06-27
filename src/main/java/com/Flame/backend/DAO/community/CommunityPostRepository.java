package com.Flame.backend.DAO.community;

import com.Flame.backend.entities.community.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Integer> {
}
