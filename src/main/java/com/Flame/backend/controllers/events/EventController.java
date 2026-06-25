package com.Flame.backend.controllers.events;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.workshop.Workshop;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.entities.event.Event;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;
    private final CustomerRepository customerRepository;

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
   }

    @GetMapping("/{id}")
    public Event getEvent(@PathVariable Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        return event;
    }

    @DeleteMapping("/{id}")
    public Event deleteWorkshop(@PathVariable Long id) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Optional<Customer> user =   customerRepository.findByEmail(authentication.getName());
        if(user.isPresent() && user.get().getEventsCreated().stream().noneMatch(workshop -> workshop.getId().equals(id))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this event");
        }
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        eventRepository.deleteById(id);
        return event;
    }
}