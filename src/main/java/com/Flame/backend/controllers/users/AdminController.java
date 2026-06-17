package com.Flame.backend.controllers.users;


import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.post.PostRepository;
import com.Flame.backend.DAO.users.ProviderRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.post.Post;
import com.Flame.backend.entities.user.Provider;
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
    private final ProviderRepository providerRepository;
    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;
    private final PostRepository postRepository;

    public record SuspensionRequest(Boolean suspended, String reason) {
    }

    private User getCurrentAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }

    private boolean toSuspendedFlag(SuspensionRequest request) {
        if (request == null || request.suspended() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "suspended flag is required");
        }
        return request.suspended();
    }

    private String toReason(SuspensionRequest request) {
        if (request == null || request.reason() == null) {
            return null;
        }
        String normalized = request.reason().trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/providers")
    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Integer id) {
        userRepository.deleteById(id);
        return "User deleted";
    }

    @DeleteMapping("/providers/{id}")
    public String deleteProvider(@PathVariable Integer id) {
        providerRepository.deleteById(id);
        return "Provider deleted";
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

    @PutMapping("/users/{id}/suspension")
    public User updateUserSuspension(
            @PathVariable Integer id,
            @RequestBody SuspensionRequest payload,
            Authentication authentication
    ) {
        User admin = getCurrentAdmin(authentication);
        if (admin.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot suspend self");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setSuspended(toSuspendedFlag(payload));
        user.setSuspensionReason(toReason(payload));
        return userRepository.save(user);
    }

    @PutMapping("/providers/{id}/suspension")
    public Provider updateProviderSuspension(
            @PathVariable Integer id,
            @RequestBody SuspensionRequest payload,
            Authentication authentication
    ) {
        User admin = getCurrentAdmin(authentication);
        if (admin.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot suspend self");
        }

        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));

        provider.setSuspended(toSuspendedFlag(payload));
        provider.setSuspensionReason(toReason(payload));
        return providerRepository.save(provider);
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

    @PutMapping("/events/{id}/suspension")
    public Event updateEventSuspension(@PathVariable Long id, @RequestBody SuspensionRequest payload) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        event.setSuspended(toSuspendedFlag(payload));
        event.setSuspensionReason(toReason(payload));
        return eventRepository.save(event);
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

    @PutMapping("/workshops/{id}/suspension")
    public Workshop updateWorkshopSuspension(@PathVariable Long id, @RequestBody SuspensionRequest payload) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workshop not found"));

        workshop.setSuspended(toSuspendedFlag(payload));
        workshop.setSuspensionReason(toReason(payload));
        return workshopRepository.save(workshop);
    }

    @GetMapping("/posts")
    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    @DeleteMapping("/posts/{id}")
    public String deletePost(@PathVariable Long id) {
        postRepository.deleteById(id);
        return "Post deleted";
    }

    @PutMapping("/posts/{id}/suspension")
    public Post updatePostSuspension(@PathVariable Long id, @RequestBody SuspensionRequest payload) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));

        post.setSuspended(toSuspendedFlag(payload));
        post.setSuspensionReason(toReason(payload));
        return postRepository.save(post);
    }
}