package com.Flame.backend.entities.user;
import com.Flame.backend.entities.Reels.Comment;
import com.Flame.backend.entities.Reels.Reel;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.workshop.Workshop;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Entity

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Customer extends User {

    private BigDecimal balance = BigDecimal.ZERO;
    private String companyName;

    /**
     * Comma-separated user preference tags, e.g. "fitness,cooking,sports".
     * Normalized to lowercase by PreferenceService.
     * Used by FeedScoringService to personalise the recommendation feed.
     */
    private String preferences;



    @ManyToMany(mappedBy = "customers", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Event> eventsBooked;

    @ManyToMany(mappedBy = "customers", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Workshop> workshopsBooked;

    @OneToMany(mappedBy="creator")
    @JsonIgnore
    private List<Reel> createdReels;

    @ManyToMany(mappedBy="likes")
    @JsonIgnore
    private Set<Reel> likedReels;

    @ManyToMany(mappedBy="savedBy")
    @JsonIgnore
    private Set<Reel> savedReels;

    @OneToMany(mappedBy="user")
    @JsonIgnore
    private List<Comment> comments;


    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL,fetch =  FetchType.EAGER)
    private List<Event> eventsCreated;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL,fetch =  FetchType.EAGER)
    private List<Workshop> workshopsCreated;

    // Getters & Setters...
}