package com.Flame.backend.controllers.users;


import com.Flame.backend.DAO.booking.EventTicketBookingRepository;
import com.Flame.backend.DAO.booking.WorkshopTicketBookingRepository;
import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.Role;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.entities.workshop.Workshop;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.webmvc.autoconfigure.WebMvcProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;
    private final EventTicketBookingRepository eventTicketBookingRepository;
    private final WorkshopTicketBookingRepository workshopTicketBookingRepository;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Integer id ,Authentication authentication) {
        User admin=(User) authentication.getPrincipal();
        if(admin.getRole()!= Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this user");
        }

        Customer customer = (Customer) userRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        eventTicketBookingRepository.deleteAll(
                eventTicketBookingRepository.findByCustomer(customer)
        );

        workshopTicketBookingRepository.deleteAll(
                workshopTicketBookingRepository.findByCustomer(customer)
        );

        userRepository.delete(customer);

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



    @GetMapping("/events/{id}")
    public Event getEvent(@PathVariable Long id) {
        return eventRepository.findById(id).orElse(null);
    }











}