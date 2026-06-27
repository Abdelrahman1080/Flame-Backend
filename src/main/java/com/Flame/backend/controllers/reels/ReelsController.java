package com.Flame.backend.controllers.reels;
import com.Flame.backend.DTO.reels.ReelResponseDTO;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.services.reels.ReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reels")
@RequiredArgsConstructor
public class ReelsController {

    private final ReelService reelService;
    @PostMapping("/upload")
    public ReelResponseDTO uploadReel(
            @RequestParam MultipartFile video,
            @RequestParam String caption,
            @RequestParam String preferences) throws IOException {

        return reelService.upload(video, caption, preferences);
    }

    @GetMapping
    public List<ReelResponseDTO> getAll() {
        return reelService.getAll();
    }

    @GetMapping("/{id}")
    public ReelResponseDTO getOne(@PathVariable Long id) {
        return reelService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        reelService.delete(id);
    }

    @PostMapping("/{id}/like")
    public void like(@PathVariable Long id) {
        reelService.toggleLike(id);
    }

    @PostMapping("/{id}/save")
    public void save(@PathVariable Long id) {
        reelService.toggleSave(id);
    }

    // Dummy upload — skips moderation, sets status to APPROVED directly
    // Use this for testing only, not for production
    @PostMapping("/upload/dummy")
    public ReelResponseDTO uploadDummy(
            @RequestParam MultipartFile video,
            @RequestParam String caption,
            @RequestParam String preferences) throws IOException {

        return reelService.uploadDummy(video, caption, preferences);
    }

}