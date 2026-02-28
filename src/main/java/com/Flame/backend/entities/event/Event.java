package com.Flame.backend.entities.event;

import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.Provider;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String location;
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @ManyToMany
    @JoinTable(
            name = "event_customers",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    @JsonManagedReference
    private List<Customer> customers;


    public Event() {}
    public Event(String title, String description, String location, Integer capacity, Provider provider) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.capacity = capacity;
        this.provider = provider;
    }



}
