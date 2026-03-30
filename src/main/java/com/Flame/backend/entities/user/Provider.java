package com.Flame.backend.entities.user;
import java.util.List;

import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.workshop.Workshop;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor //constructor with no args
@AllArgsConstructor //constructor with all args
public class Provider extends User {

    private String companyName;
    private String companyLogoUrl;
    private String companyTagline;

    @Column(length = 2000)
    private String companyDescription;

    private String companyWebsite;
    private String companyLocation;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL,fetch =  FetchType.EAGER)
    private List<Event> eventsCreated;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL,fetch =  FetchType.EAGER)
    private List<Workshop> workshopsCreated;



    // Getters & Setters...
}