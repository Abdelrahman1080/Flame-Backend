package com.Flame.backend.services.reels;

import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DTO.reels.ReelResponseDTO;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.mappers.ReelMapper;
import com.Flame.backend.services.PreferenceService;
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
                .build();

        Reel saved = reelRepository.save(reel);

        return ReelMapper.toDTO(saved, customer);
    }

    // ---------------- GET ALL ----------------

    @Override
    public List<ReelResponseDTO> getAll() {

        Customer customer = getCurrentUser();

        return reelRepository.findAll()
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

    // ---------------- AUTH ----------------

    private Customer getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }
}