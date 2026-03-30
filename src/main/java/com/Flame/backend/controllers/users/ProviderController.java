package com.Flame.backend.controllers.users;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.ProviderRepository;
import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Provider;
import com.Flame.backend.entities.workshop.Workshop;
import com.Flame.backend.services.PreferenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;
    private final ProviderRepository providerRepository;
    private final PreferenceService preferenceService;

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void normalizeEventDateRange(Event event) {
        if (event.getStartDate() == null && event.getEndDate() != null) {
            event.setStartDate(event.getEndDate());
        }
        if (event.getEndDate() == null && event.getStartDate() != null) {
            event.setEndDate(event.getStartDate());
        }
    }

    private void normalizeWorkshopDateRange(Workshop workshop) {
        if (workshop.getStartDate() == null && workshop.getEndDate() != null) {
            workshop.setStartDate(workshop.getEndDate());
        }
        if (workshop.getEndDate() == null && workshop.getStartDate() != null) {
            workshop.setEndDate(workshop.getStartDate());
        }
    }

    @GetMapping("/page")
    public Provider getMyPage(Authentication authentication) {
        return (Provider) authentication.getPrincipal();
    }

    @PutMapping("/page")
    public Provider updateMyPage(@RequestBody Map<String, String> payload, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();

        provider.setCompanyName(payload.getOrDefault("companyName", provider.getCompanyName()));
        provider.setCompanyLogoUrl(payload.getOrDefault("companyLogoUrl", provider.getCompanyLogoUrl()));
        provider.setCompanyTagline(payload.getOrDefault("companyTagline", provider.getCompanyTagline()));
        provider.setCompanyDescription(payload.getOrDefault("companyDescription", provider.getCompanyDescription()));
        provider.setCompanyWebsite(payload.getOrDefault("companyWebsite", provider.getCompanyWebsite()));
        provider.setCompanyLocation(payload.getOrDefault("companyLocation", provider.getCompanyLocation()));

        return providerRepository.save(provider);
    }

        @PostMapping(value = {"/page/logo", "/profile-image"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Provider uploadMyCompanyLogo(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file uploaded");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "File too large. Max size is 10MB");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        String originalName = file.getOriginalFilename() == null ? "company-logo" : file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        Path uploadDir = Paths.get("uploads", "company-logos").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Provider provider = (Provider) authentication.getPrincipal();
        provider.setCompanyLogoUrl("/uploads/company-logos/" + filename);
        return providerRepository.save(provider);
    }

    @PostMapping("/events")
    public Event createEvent(@RequestBody Event event, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        event.setProvider(provider);
        event.setCategory(preferenceService.normalize(event.getCategory()));
        preferenceService.ensureExists(event.getCategory());
        event.setImageUrl(normalizeOptionalText(event.getImageUrl()));
        normalizeEventDateRange(event);
        return eventRepository.save(event);
    }

    @PostMapping("/workshops")
    public Workshop createWorkshop(@RequestBody Workshop workshop, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        workshop.setProvider(provider);
        workshop.setCategory(preferenceService.normalize(workshop.getCategory()));
        preferenceService.ensureExists(workshop.getCategory());
        workshop.setImageUrl(normalizeOptionalText(workshop.getImageUrl()));
        normalizeWorkshopDateRange(workshop);
        return workshopRepository.save(workshop);
    }

    @GetMapping("/events")
    public List<Event> getMyEvents(Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        return eventRepository.findByProvider(provider);
    }

    @GetMapping("/events/{id}")
    public Event getMyEventById(@PathVariable Long id, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getProvider() == null || !event.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("You cannot view this event");
        }

        return event;
    }

    @GetMapping("/workshops")
    public List<Workshop> getMyWorkshops(Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        return workshopRepository.findByProvider(provider);
    }

    @GetMapping("/workshops/{id}")
    public Workshop getMyWorkshopById(@PathVariable Long id, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        if (workshop.getProvider() == null || !workshop.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("You cannot view this workshop");
        }

        return workshop;
    }

    @PutMapping("/events/{id}")
    public Event updateEvent(@PathVariable Long id, @RequestBody Event event, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (existing.getProvider() == null || !existing.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("You cannot update this event");
        }

        existing.setTitle(event.getTitle());
        existing.setDescription(event.getDescription());
        existing.setLocation(event.getLocation());
        existing.setCategory(preferenceService.normalize(event.getCategory()));
        preferenceService.ensureExists(existing.getCategory());
        existing.setImageUrl(normalizeOptionalText(event.getImageUrl()));
        existing.setStartDate(event.getStartDate());
        existing.setEndDate(event.getEndDate());
        normalizeEventDateRange(existing);
        existing.setCapacity(event.getCapacity());
        return eventRepository.save(existing);
    }

    @PutMapping("/workshops/{id}")
    public Workshop updateWorkshop(@PathVariable Long id, @RequestBody Workshop workshop, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        Workshop existing = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        if (existing.getProvider() == null || !existing.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("You cannot update this workshop");
        }

        existing.setTitle(workshop.getTitle());
        existing.setDescription(workshop.getDescription());
        existing.setLocation(workshop.getLocation());
        existing.setCategory(preferenceService.normalize(workshop.getCategory()));
        preferenceService.ensureExists(existing.getCategory());
        existing.setImageUrl(normalizeOptionalText(workshop.getImageUrl()));
        existing.setStartDate(workshop.getStartDate());
        existing.setEndDate(workshop.getEndDate());
        normalizeWorkshopDateRange(existing);
        existing.setCapacity(workshop.getCapacity());
        return workshopRepository.save(existing);
    }

    @DeleteMapping("/events/{id}")
    public String deleteEvent(@PathVariable Long id, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (existing.getProvider() == null || !existing.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("You cannot delete this event");
        }

        eventRepository.delete(existing);
        return "Event deleted";
    }

    @DeleteMapping("/workshops/{id}")
    public String deleteWorkshop(@PathVariable Long id, Authentication authentication) {
        Provider provider = (Provider) authentication.getPrincipal();
        Workshop existing = workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        if (existing.getProvider() == null || !existing.getProvider().getId().equals(provider.getId())) {
            throw new RuntimeException("You cannot delete this workshop");
        }

        workshopRepository.delete(existing);
        return "workshop deleted";
    }
}