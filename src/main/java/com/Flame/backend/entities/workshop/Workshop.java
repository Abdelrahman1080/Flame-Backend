package com.Flame.backend.entities.workshop;
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

 public class Workshop {

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
            name = "workshop_customers",
            joinColumns = @JoinColumn(name = "workshop_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    private List<Customer> customers;

    // Constructors
    public Workshop() {}
    public Workshop(String title, String description, String location, Integer capacity, Provider provider) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.capacity = capacity;
        this.provider = provider;
    }

    // Getters & Setters...
}