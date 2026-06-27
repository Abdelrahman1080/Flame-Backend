package com.Flame.backend.DAO.community;

import com.Flame.backend.entities.community.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Integer> {
}
