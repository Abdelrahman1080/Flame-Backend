package com.Flame.backend.services.feed;

import com.Flame.backend.DAO.feed.CustomerSeenReelRepository;
import com.Flame.backend.DAO.feed.ReelInteractionRepository;
import com.Flame.backend.DAO.reels.ReelRepository;
import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DTO.feed.InteractionRequestDTO;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.feed.CustomerSeenReel;
import com.Flame.backend.entities.feed.ReelInteraction;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.enums.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InteractionServiceImpl implements InteractionService {

    private final ReelInteractionRepository interactionRepository;
    private final CustomerSeenReelRepository seenReelRepository;
    private final ReelRepository reelRepository;
    private final CustomerRepository customerRepository;

    @Override
    public void recordInteraction(Long reelId, InteractionRequestDTO request, String userEmail) {
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + userEmail));

        Reel reel = reelRepository.findById(reelId)
                .orElseThrow(() -> new RuntimeException("Reel not found"));

        Double completionRate = 0.0;
        if (request.getWatchDurationMs() != null && request.getReelDurationMs() != null && request.getReelDurationMs() > 0) {
            completionRate = (double) request.getWatchDurationMs() / request.getReelDurationMs();
            // clamp between 0 and 1
            completionRate = Math.max(0.0, Math.min(1.0, completionRate));
        }

        ReelInteraction interaction = ReelInteraction.builder()
                .customer(customer)
                .reel(reel)
                .interactionType(request.getInteractionType())
                .watchDurationMs(request.getWatchDurationMs())
                .reelDurationMs(request.getReelDurationMs())
                .completionRate(completionRate)
                .build();
        
        interactionRepository.save(interaction);

        // Mark as seen if it's a viewing-related interaction
        InteractionType type = request.getInteractionType();
        if (type == InteractionType.IMPRESSION || type == InteractionType.WATCH || 
            type == InteractionType.COMPLETE || type == InteractionType.SKIP || 
            type == InteractionType.NOT_INTERESTED) {
            
            if (!seenReelRepository.existsByCustomerAndReel(customer, reel)) {
                try {
                    CustomerSeenReel seen = CustomerSeenReel.builder()
                            .customer(customer)
                            .reel(reel)
                            .build();
                    seenReelRepository.save(seen);
                } catch (DataIntegrityViolationException e) {
                    // Unique constraint violated - ignore (race condition or concurrent save)
                    log.warn("Duplicate seen reel ignored for customer {} and reel {}", customer.getId(), reel.getId());
                }
            }
        }
    }
}
