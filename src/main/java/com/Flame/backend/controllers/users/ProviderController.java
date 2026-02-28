package com.Flame.backend.controllers.users;


import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.Provider;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.entities.workshop.Workshop;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PROVIDER','ADMIN')")
public class ProviderController {

    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;

    @PostMapping("/events")
    public Event createEvent(@RequestBody Event event, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        event.setProvider(provider);
        return eventRepository.save(event);
    }

    @PostMapping("/workshops")
    public Workshop createWorkshop(@RequestBody Workshop workshop, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        workshop.setProvider(provider);
        return workshopRepository.save(workshop);
    }

    @GetMapping("/events")
    public List<Event> getMyEvents(Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        return eventRepository.findByProvider(provider);
    }

    @GetMapping("/workshops")
    public List<Event> getMyWorkshops(Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        return workshopRepository.findByProvider(provider);
    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@PathVariable Long id) {
        eventRepository.deleteById(id);
        return "Event deleted";
    }

    @DeleteMapping("/workshops/{id}")
    public String deleteWorkshop(@PathVariable Long id) {
        workshopRepository.deleteById(id);
        return "workshop deleted";
    }
}