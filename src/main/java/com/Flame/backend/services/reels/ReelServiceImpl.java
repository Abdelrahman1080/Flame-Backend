package com.Flame.backend.services.reels;

import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DTO.reels.ReelResponseDTO;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.ReelStatus;
import com.Flame.backend.mappers.ReelMapper;
import com.Flame.backend.services.PreferenceService;
import com.Flame.backend.services.reelsModeration.ReelModerationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReelServiceImpl implements ReelService {

    private final ReelRepository reelRepository;
    private final CustomerRepository customerRepository;
    private final PreferenceService preferenceService;
    private final ReelModerationService reelModerationService; // NEW

    // ---------------- UPLOAD ----------------

    @Override
    public ReelResponseDTO upload(MultipartFile video,
                                  String caption,
                                  String preferences) throws IOException {

        Customer customer = getCurrentUser();

        String filename = UUID.randomUUID() + "_" + video.getOriginalFilename();

        Path uploadDir = Paths.get("uploads/reels");

        if (!Files.exists(uploadDir))
            Files.createDirectories(uploadDir);

        Files.copy(
                video.getInputStream(),
                uploadDir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING
        );

        Reel reel = Reel.builder()
                .caption(caption)
                .videoUrl("/uploads/reels/" + filename)
                .creator(customer)
                .preferences(
                        preferenceService.mergeCsvPreferences("", preferences)
                )
                .status(ReelStatus.PENDING_REVIEW) // NEW — starts as pending
                .build();

        Reel saved = reelRepository.save(reel);

        // NEW — trigger background AI moderation scan
        // user gets instant response, scan runs in background thread
        reelModerationService.scanAndPersistAsync(saved, "reels/" + filename);

        return ReelMapper.toDTO(saved, customer);
    }

    // ---------------- GET ALL ----------------

    @Override
    public List<ReelResponseDTO> getAll() {

        Customer customer = getCurrentUser();

        // NEW — only return APPROVED reels to normal users
        return reelRepository.findByStatus(ReelStatus.APPROVED)
                .stream()
                .map(reel -> ReelMapper.toDTO(reel, customer))
                .toList();
    }

    // ---------------- GET BY ID ----------------

    @Override
    public ReelResponseDTO getById(Long id) {

        Customer customer = getCurrentUser();

        Reel reel = reelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        // NEW — block access to non-approved reels
        if (!reel.isPubliclyVisible()) {
            throw new RuntimeException("Reel is not available.");
        }

        return ReelMapper.toDTO(reel, customer);
    }

    // ---------------- DELETE ----------------

    @Override
    public void delete(Long id) {

        Customer customer = getCurrentUser();

        Reel reel = reelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        if (!reel.getCreator().getId().equals(customer.getId()))
            throw new RuntimeException("Not allowed");

        reelRepository.delete(reel);
    }

    // ---------------- LIKE ----------------

    @Override
    public void toggleLike(Long reelId) {

        Customer customer = getCurrentUser();

        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        if (reel.getLikes().contains(customer))
            reel.getLikes().remove(customer);
        else
            reel.getLikes().add(customer);

        reelRepository.save(reel);
    }

    // ---------------- SAVE ----------------

    @Override
    public void toggleSave(Long reelId) {

        Customer customer = getCurrentUser();

        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        if (reel.getSavedBy().contains(customer))
            reel.getSavedBy().remove(customer);
        else
            reel.getSavedBy().add(customer);

        reelRepository.save(reel);
    }

    // ---------------- UPLOAD DUMMY (no moderation) ----------------

    @Override
    public ReelResponseDTO uploadDummy(MultipartFile video,
                                       String caption,
                                       String preferences) throws IOException {

        Customer customer = getCurrentUser();

        String filename = UUID.randomUUID() + "_" + video.getOriginalFilename();

        Path uploadDir = Paths.get("uploads/reels");
        if (!Files.exists(uploadDir))
            Files.createDirectories(uploadDir);

        Files.copy(
                video.getInputStream(),
                uploadDir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING
        );

        Reel reel = Reel.builder()
                .caption(caption)
                .videoUrl("/uploads/reels/" + filename)
                .creator(customer)
                .preferences(
                        preferenceService.mergeCsvPreferences("", preferences)
                )
                .status(ReelStatus.APPROVED) // skip moderation, approve directly
                .build();

        return ReelMapper.toDTO(reelRepository.save(reel), customer);
    }

    // ---------------- AUTH ----------------

    private Customer getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }
}