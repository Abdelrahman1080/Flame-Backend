package com.Flame.backend.entities.Reels;


import com.Flame.backend.entities.user.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caption;

    private String videoUrl;

    private String thumbnailUrl;

    private Integer durationSeconds;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="creator_id")
    private Customer creator;

    @ManyToMany
    @Builder.Default
    @JoinTable(
            name="reel_likes",
            joinColumns=@JoinColumn(name="reel_id"),
            inverseJoinColumns=@JoinColumn(name="customer_id")
    )
    private Set<Customer> likes = new HashSet<>();

    @ManyToMany
    @Builder.Default
    @JoinTable(
            name="reel_saves",
            joinColumns=@JoinColumn(name="reel_id"),
            inverseJoinColumns=@JoinColumn(name="customer_id")
    )
    private Set<Customer> savedBy = new HashSet<>();

    @OneToMany(mappedBy="reel",cascade=CascadeType.ALL)
    @Builder.Default
    private List<Comment> comments=new ArrayList<>();

    private String preferences;
}