package com.Flame.backend.services.reels;

import com.Flame.backend.DTO.reels.ReelResponseDTO;
import com.Flame.backend.entities.Reels.Reel;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ReelService {

    ReelResponseDTO upload(MultipartFile video,
                           String caption,
                           String preferences) throws IOException;

    List<ReelResponseDTO> getAll();


    ReelResponseDTO getById(Long id);

    void delete(Long id);

    void toggleLike(Long reelId);

    void toggleSave(Long reelId);

    ReelResponseDTO uploadDummy(MultipartFile video,
                                String caption,
                                String preferences) throws IOException;
    List<ReelResponseDTO> getReelsByUser(Integer userId);
}