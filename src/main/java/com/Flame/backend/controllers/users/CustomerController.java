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
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')") // ضمان أن ده للـ Customers بس
public class CustomerController {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;

    @GetMapping("/me")
    public User getMyProfile(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    @PostMapping("/events/{eventId}/book")
    @Transactional
    public String bookEvent(@PathVariable Long eventId, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getCustomers().contains(customer)) {
            event.getCustomers().add(customer);
        }
        if (!customer.getEventsBooked().contains(event)) {
            customer.getEventsBooked().add(event);
        }

        eventRepository.save(event); // حفظ العلاقة
        return "Event booked successfully";
    }

    @PostMapping("/workshops/{workshopId}/book")
    @Transactional
    public String bookWorkshop(@PathVariable Long workshopId, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        if (!workshop.getCustomers().contains(customer)) {
            workshop.getCustomers().add(customer);
        }
        if (!customer.getWorkshopsBooked().contains(workshop)) {
            customer.getWorkshopsBooked().add(workshop);
        }

        workshopRepository.save(workshop); // حفظ العلاقة
        return "Workshop booked successfully";
    }
}