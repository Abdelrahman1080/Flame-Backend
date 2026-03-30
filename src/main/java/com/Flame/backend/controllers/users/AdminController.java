package com.Flame.backend.controllers.users;


import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Role;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.entities.workshop.Workshop;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Integer id) {
        userRepository.deleteById(id);
        return "User deleted";
    }

    @PutMapping("/users/{id}/role")
    public User updateUserRole(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        String newRole = payload.get("role");
        if (newRole == null || newRole.isBlank()) {
            throw new RuntimeException("Role is required");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(Role.valueOf(newRole.toUpperCase()));
        return userRepository.save(user);
    }

    @GetMapping("/events")
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @PutMapping("/events/{id}")
    public Event updateEvent(@PathVariable Long id, @RequestBody Event event) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        existing.setTitle(event.getTitle());
        existing.setDescription(event.getDescription());
        existing.setLocation(event.getLocation());
        existing.setImageUrl(event.getImageUrl());
        existing.setStartDate(event.getStartDate());
        existing.setEndDate(event.getEndDate());
        existing.setCapacity(event.getCapacity());
        return eventRepository.save(existing);
    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@PathVariable Long id) {
        eventRepository.deleteById(id);
        return "Event deleted";
    }

    @GetMapping("/workshops")
    public List<Workshop> getAllWorkshops() {
        return workshopRepository.findAll();
    }

    @PutMapping("/workshops/{id}")
    public Workshop updateWorkshop(@PathVariable Long id, @RequestBody Workshop workshop) {
        Workshop existing = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        existing.setTitle(workshop.getTitle());
        existing.setDescription(workshop.getDescription());
        existing.setLocation(workshop.getLocation());
        existing.setImageUrl(workshop.getImageUrl());
        existing.setStartDate(workshop.getStartDate());
        existing.setEndDate(workshop.getEndDate());
        existing.setCapacity(workshop.getCapacity());
        return workshopRepository.save(existing);
    }

    @DeleteMapping("/workshops/{id}")
    public String deleteWorkshop(@PathVariable Long id) {
        workshopRepository.deleteById(id);
        return "Workshop deleted";
    }
}