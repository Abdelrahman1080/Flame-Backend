package com.Flame.backend.entities.feed;

import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.user.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_seen_reels", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"customer_id", "reel_id"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CustomerSeenReel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reel_id", nullable = false)
    private Reel reel;

    @Column(nullable = false, updatable = false)
    private LocalDateTime seenAt;

    @PrePersist
    protected void onCreate() {
        this.seenAt = LocalDateTime.now();
    }
}
