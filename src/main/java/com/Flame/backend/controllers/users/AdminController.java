package com.Flame.backend.controllers.users;


import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Customer;
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

    @GetMapping("/events")
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/workshops")
    public List<Workshop> getAllWorkshops() {
        return workshopRepository.findAll();
    }
}