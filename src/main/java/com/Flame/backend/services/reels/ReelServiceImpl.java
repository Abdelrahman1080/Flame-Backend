package com.Flame.backend.services.reels;

import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DTO.reels.ReelResponseDTO;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.ReelStatus;
import com.Flame.backend.mappers.ReelMapper;
import com.Flame.backend.services.reelsModeration.GcsFileUploadService;
import com.Flame.backend.services.PreferenceService;
import com.Flame.backend.services.reelsModeration.ReelModerationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReelServiceImpl implements ReelService {

    private final ReelRepository reelRepository;
    private final CustomerRepository customerRepository;
    private final PreferenceService preferenceService;
    private final ReelModerationService reelModerationService;
    private final GcsFileUploadService gcsFileUploadService;

    // ---------------- UPLOAD ----------------

    @Override
    public ReelResponseDTO upload(MultipartFile video,
                                  String caption,
                                  String preferences) throws IOException {

        Customer customer = getCurrentUser();

        // Upload directly to GCS, no local save
        String videoUrl = gcsFileUploadService.uploadVideo(video, "reels");

        Reel reel = Reel.builder()
                .caption(caption)
                .videoUrl(videoUrl)
                .creator(customer)
                .preferences(
                        preferenceService.mergeCsvPreferences("", preferences)
                )
                .status(ReelStatus.PENDING_REVIEW)
                .build();

        Reel saved = reelRepository.save(reel);

        // Trigger background AI moderation scan using the GCS URL
        // Convert public URL to gs:// URI for Video Intelligence API
        reelModerationService.scanAndPersistAsync(saved, toGcsUri(videoUrl));

        return ReelMapper.toDTO(saved, customer);
    }

    // ---------------- UPLOAD DUMMY (no moderation) ----------------

    @Override
    public ReelResponseDTO uploadDummy(MultipartFile video,
                                       String caption,
                                       String preferences) throws IOException {

        Customer customer = getCurrentUser();

        // Upload directly to GCS, no local save
        String videoUrl = gcsFileUploadService.uploadVideo(video, "reels");

        Reel reel = Reel.builder()
                .caption(caption)
                .videoUrl(videoUrl)
                .creator(customer)
                .preferences(
                        preferenceService.mergeCsvPreferences("", preferences)
                )
                .status(ReelStatus.APPROVED) // skip moderation
                .build();

        return ReelMapper.toDTO(reelRepository.save(reel), customer);
    }

    // ---------------- GET ALL ----------------

    @Override
    public List<ReelResponseDTO> getAll() {
        Customer customer = getCurrentUser();
        return reelRepository.findByStatus(ReelStatus.APPROVED)
                .stream()
                .map(reel -> ReelMapper.toDTO(reel, customer))
                .toList();
    }


    // get By Customer
    @Override
    public List<ReelResponseDTO> getReelsByUser(Integer userId) {


        Customer customer = getCurrentUser();

        // NEW — only return APPROVED reels to normal users
        return reelRepository.findByStatusAndCreator(ReelStatus.APPROVED, customer)
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

        if (!reel.isPubliclyVisible())
            throw new RuntimeException("Reel is not available.");

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

        // Delete from GCS
        gcsFileUploadService.deleteFile(reel.getVideoUrl());
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

    // ---------------- HELPERS ----------------

    /**
     * Converts a public GCS URL to a gs:// URI for the Video Intelligence API.
     * e.g. https://storage.googleapis.com/my-bucket/reels/abc.mp4
     *   -> gs://my-bucket/reels/abc.mp4
     */
    private String toGcsUri(String publicUrl) {
        // Extract everything after "https://storage.googleapis.com/"
        String withoutHost = publicUrl.replace("https://storage.googleapis.com/", "");
        return "gs://" + withoutHost;
    }
    @Override
    public List<ReelResponseDTO> getLiked() {
        Customer customer = getCurrentUser();

        return reelRepository.findByLikesContaining(customer)
                .stream()
                .map(reel -> ReelMapper.toDTO(reel, customer))
                .toList();
    }

    @Override
    public List<ReelResponseDTO> getSaved() {
        Customer customer = getCurrentUser();

        return reelRepository.findBySavedByContaining(customer)
                .stream()
                .map(reel -> ReelMapper.toDTO(reel, customer))
                .toList();
    }
}