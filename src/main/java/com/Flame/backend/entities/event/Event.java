package com.Flame.backend.entities.event;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.Hibernate;

import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.Provider;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

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
    private String category;
    private String imageUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "provider_id")
    private Provider provider;


        @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "event_customers",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "customer_id")
    )
    private List<Customer> customers;







    public Event() {}
    public Event(String title, String description, String location, String category, String imageUrl, LocalDate startDate, LocalDate endDate, Integer capacity, Provider provider) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.category = category;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        this.provider = provider;
    }

    @JsonProperty("providerId")
    public Integer getProviderId() {
        return provider == null ? null : provider.getId();
    }

    @JsonProperty("providerCompanyName")
    public String getProviderCompanyName() {
        if (provider == null || !Hibernate.isInitialized(provider)) {
            return null;
        }
        return provider.getCompanyName();
    }



}
