package com.Flame.backend.entities.user;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.workshop.Workshop;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Entity

@SuperBuilder
@NoArgsConstructor //constructor with no args
@AllArgsConstructor //constructor with all args
@Getter
@Setter
public class Customer extends User {

    private BigDecimal balance = BigDecimal.ZERO;



    @ManyToMany(mappedBy = "customers", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Event> eventsBooked;

    @ManyToMany(mappedBy = "customers", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Workshop> workshopsBooked;


    // Getters & Setters...
}