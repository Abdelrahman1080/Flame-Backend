package com.Flame.backend.controllers.users;


import com.Flame.backend.DAO.booking.EventTicketBookingRepository;
import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.entities.booking.WorkshopTicketBooking;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.entities.workshop.Workshop;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.Flame.backend.DAO.booking.WorkshopTicketBookingRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
public class ProviderController {

    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;
    private final WorkshopTicketBookingRepository workshopTicketBookingRepository;
    private final EventTicketBookingRepository eventTicketBookingRepository;

    @PostMapping("/events")
    public Event createEvent(@RequestBody Event event, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        event.setProvider(customer);
        return eventRepository.save(event);
    }

    @PostMapping("/workshops")
    public Workshop createWorkshop(@RequestBody Workshop workshop, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        workshop.setProvider(customer);
        return workshopRepository.save(workshop);
    }

    @GetMapping("/events")
    public List<Event> getMyEvents(Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        return eventRepository.findByProvider(customer);
    }

    @GetMapping("/workshops")
    public List<Workshop> getMyWorkshops(Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        return workshopRepository.findByProvider(customer);
    }

    @GetMapping("/events-customers/{eventId}")
    public List<Customer> getEventCustomers(@PathVariable Long eventId,Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();


        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if(event.getProvider().getId() != customer.getId()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this event's customers");
        }

        return event.getCustomers();

    }

    @GetMapping("/workshops-customers/{eventId}")
    public List<Customer> getWorkshopsCustomers(@PathVariable Long eventId,Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();


        Workshop workshop = workshopRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));
        if(workshop.getProvider().getId() != customer.getId()){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this workshop's customers");
        }

        return workshop.getCustomers();

    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@PathVariable Long id, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getProvider().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this event");
        }
        eventTicketBookingRepository.delete(eventTicketBookingRepository.findByEvent(event));
        eventRepository.delete(event);
        return "Event deleted";
    }


    @DeleteMapping("/workshops/{id}")
    public String deleteWorkshop(@PathVariable Long id, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        if (!workshop.getProvider().getId().equals(customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this workshop");
        }
        workshopTicketBookingRepository.delete(workshopTicketBookingRepository.findByWorkshop(workshop));
        workshopRepository.delete(workshop);
        return "Workshop deleted";
    }

}