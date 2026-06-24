package com.Flame.backend.controllers.workshop;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.workshop.Workshop;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workshops")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopRepository workshopRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @GetMapping
    public List<Workshop> getAllWorkshops() {
        return workshopRepository.findAll();
    }



    @GetMapping("/{id}")
    public Workshop getWorkshop(@PathVariable Long id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workshop not found"));

        return workshop;
    }

    @DeleteMapping("/{id}")
    public Workshop deleteWorkshop(@PathVariable Long id) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Optional<Customer> user =   customerRepository.findByEmail(authentication.getName());
        if(user.isPresent() && user.get().getWorkshopsCreated().stream().noneMatch(workshop -> workshop.getId().equals(id))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to delete this workshop");
        }
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workshop not found"));
     workshopRepository.deleteById(id);
     return workshop;
    }
}