package com.Flame.backend.DAO.community;

import com.Flame.backend.entities.community.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Integer> {
    List<Community> findByAdminId(Integer adminId);
    List<Community> findByMembers_Id(Integer userId);
}
