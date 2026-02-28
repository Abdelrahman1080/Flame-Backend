package com.Flame.backend.entities.user;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.workshop.Workshop;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor //constructor with no args
@AllArgsConstructor //constructor with all args
public class Provider extends User {

    private String companyName; // Optional: اسم الشركة أو وصف

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL,fetch =  FetchType.EAGER)
    private List<Event> eventsCreated;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL,fetch =  FetchType.EAGER)
    private List<Workshop> workshopsCreated;



    // Getters & Setters...
}