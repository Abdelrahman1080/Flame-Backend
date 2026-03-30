package com.Flame.backend.controllers.users;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

import com.Flame.backend.DAO.booking.EventTicketBookingRepository;
import com.Flame.backend.DAO.booking.WorkshopTicketBookingRepository;
import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.UserRepository;
import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.booking.EventTicketBooking;
import com.Flame.backend.entities.booking.WorkshopTicketBooking;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.User;
import com.Flame.backend.entities.workshop.Workshop;
import com.Flame.backend.services.PreferenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')") // ضمان أن ده للـ Customers بس
public class CustomerController {

    private static final int MAX_PREFERENCES = 5;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;
    private final EventTicketBookingRepository eventTicketBookingRepository;
    private final WorkshopTicketBookingRepository workshopTicketBookingRepository;
    private final UserRepository userRepository;
    private final PreferenceService preferenceService;

    private List<String> extractTicketEmails(Map<String, Object> payload, String fallbackEmail, int ticketCount) {
        List<String> emails = new ArrayList<>();

        Object multi = payload == null ? null : payload.get("ticketEmails");
        if (multi instanceof List<?> multiList) {
            for (Object entry : multiList) {
                if (entry == null) {
                    continue;
                }
                String normalized = entry.toString().trim().toLowerCase();
                if (!normalized.isEmpty()) {
                    emails.add(normalized);
                }
            }
        }

        if (emails.isEmpty()) {
            Object single = payload == null ? null : payload.get("bookingEmail");
            String normalizedSingle = single == null ? fallbackEmail : single.toString().trim().toLowerCase();
            for (int i = 0; i < ticketCount; i++) {
                emails.add(normalizedSingle);
            }
        }

        if (emails.size() != ticketCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketEmails count must match ticketCount");
        }

        return emails;
    }

    private String generateTicketId(String prefix) {
        String compact = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + "-" + compact.substring(0, 12);
    }

    @GetMapping("/me")
    public User getMyProfile(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public User uploadMyProfileImage(
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

        String originalName = file.getOriginalFilename() == null ? "profile" : file.getOriginalFilename();
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        Path uploadDir = Paths.get("uploads", "profile-images").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        String filename = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Customer customer = (Customer) authentication.getPrincipal();
        customer.setProfileImageUrl("/uploads/profile-images/" + filename);
        return userRepository.save(customer);
    }

    @PutMapping("/preferences")
    @Transactional
    public User updateMyPreferences(Authentication authentication, @RequestBody Map<String, String> payload) {
        Customer customer = (Customer) authentication.getPrincipal();
        String incoming = payload.getOrDefault("preferences", "");
        List<String> normalized = preferenceService.splitAndNormalize(incoming);
        if (normalized.size() > MAX_PREFERENCES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can select up to 5 preferences only");
        }
        preferenceService.ensureAllExist(normalized);

        customer.setPreferences(String.join(",", normalized));
        return userRepository.save(customer);
    }

    @GetMapping("/recommendations")
    public Map<String, Object> getRecommendations(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit
    ) {
        Customer customer = (Customer) authentication.getPrincipal();
        String preferences = customer.getPreferences() == null ? "" : customer.getPreferences();
        int safeLimit = Math.max(1, Math.min(limit, 50));

        return Map.of(
                "preferences", preferences,
                "events", eventRepository.findRecommendedEvents(preferences, safeLimit),
                "workshops", workshopRepository.findRecommendedWorkshops(preferences, safeLimit)
        );
    }

    @GetMapping("/events/booked")
    public List<Map<String, Object>> getBookedEvents(Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        List<EventTicketBooking> bookings = eventTicketBookingRepository.findVisibleBookings(customer, customer.getEmail());
        return bookings.stream().map(booking -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId", booking.getId());
            row.put("ticketId", booking.getTicketId());
            row.put("bookingEmail", booking.getBookingEmail());
            row.put("ticketCount", booking.getTicketCount());
            row.put("createdAt", booking.getCreatedAt());
            row.put("item", booking.getEvent());
            return row;
        }).toList();
    }

    @GetMapping("/workshops/booked")
    public List<Map<String, Object>> getBookedWorkshops(Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        List<WorkshopTicketBooking> bookings = workshopTicketBookingRepository.findVisibleBookings(customer, customer.getEmail());
        return bookings.stream().map(booking -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bookingId", booking.getId());
            row.put("ticketId", booking.getTicketId());
            row.put("bookingEmail", booking.getBookingEmail());
            row.put("ticketCount", booking.getTicketCount());
            row.put("createdAt", booking.getCreatedAt());
            row.put("item", booking.getWorkshop());
            return row;
        }).toList();
    }

    @PostMapping("/events/{eventId}/book")
    @Transactional
    public Map<String, Object> bookEvent(
            @PathVariable Long eventId,
            Authentication authentication,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        Customer customer = (Customer) authentication.getPrincipal();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        int ticketCount = 1;
        String bookingEmail = customer.getEmail();

        if (payload != null) {
            Object quantityObj = payload.get("ticketCount");
            if (quantityObj != null) {
                try {
                    ticketCount = Integer.parseInt(quantityObj.toString());
                } catch (NumberFormatException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketCount must be a number");
                }
            }
            Object emailObj = payload.get("bookingEmail");
            if (emailObj != null && !emailObj.toString().trim().isEmpty()) {
                bookingEmail = emailObj.toString().trim().toLowerCase();
            }
        }

        if (ticketCount < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketCount must be at least 1");
        }

        List<String> ticketEmails = extractTicketEmails(payload, bookingEmail, ticketCount);

        if (!event.getCustomers().contains(customer)) {
            event.getCustomers().add(customer);
        }
        if (!customer.getEventsBooked().contains(event)) {
            customer.getEventsBooked().add(event);
        }

        eventRepository.save(event); // حفظ العلاقة

        EventTicketBooking last = null;
        List<String> generatedTicketIds = new ArrayList<>();
        for (String email : ticketEmails) {
            EventTicketBooking booking = new EventTicketBooking();
            booking.setCustomer(customer);
            booking.setEvent(event);
            booking.setBookingEmail(email);
            booking.setTicketCount(1);
            booking.setCreatedAt(LocalDateTime.now());
            booking.setTicketId(generateTicketId("EVT"));
            last = eventTicketBookingRepository.save(booking);
            generatedTicketIds.add(last.getTicketId());
        }

        return Map.of(
            "bookingId", last == null ? null : last.getId(),
                "message", "Event booked successfully",
            "ticketCount", ticketCount,
            "ticketEmails", ticketEmails,
            "ticketIds", generatedTicketIds
        );
    }

    @DeleteMapping("/events/{eventId}/book")
    @Transactional
    public String cancelEventBooking(@PathVariable Long eventId, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (event.getCustomers() != null) {
            event.getCustomers().remove(customer);
        }
        if (customer.getEventsBooked() != null) {
            customer.getEventsBooked().remove(event);
        }

        eventRepository.save(event);
        return "Event booking canceled";
    }

    @PostMapping("/workshops/{workshopId}/book")
    @Transactional
    public Map<String, Object> bookWorkshop(
            @PathVariable Long workshopId,
            Authentication authentication,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        Customer customer = (Customer) authentication.getPrincipal();
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        int ticketCount = 1;
        String bookingEmail = customer.getEmail();

        if (payload != null) {
            Object quantityObj = payload.get("ticketCount");
            if (quantityObj != null) {
                try {
                    ticketCount = Integer.parseInt(quantityObj.toString());
                } catch (NumberFormatException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketCount must be a number");
                }
            }
            Object emailObj = payload.get("bookingEmail");
            if (emailObj != null && !emailObj.toString().trim().isEmpty()) {
                bookingEmail = emailObj.toString().trim().toLowerCase();
            }
        }

        if (ticketCount < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketCount must be at least 1");
        }

        List<String> ticketEmails = extractTicketEmails(payload, bookingEmail, ticketCount);

        if (!workshop.getCustomers().contains(customer)) {
            workshop.getCustomers().add(customer);
        }
        if (!customer.getWorkshopsBooked().contains(workshop)) {
            customer.getWorkshopsBooked().add(workshop);
        }

        workshopRepository.save(workshop); // حفظ العلاقة

        WorkshopTicketBooking last = null;
        List<String> generatedTicketIds = new ArrayList<>();
        for (String email : ticketEmails) {
            WorkshopTicketBooking booking = new WorkshopTicketBooking();
            booking.setCustomer(customer);
            booking.setWorkshop(workshop);
            booking.setBookingEmail(email);
            booking.setTicketCount(1);
            booking.setCreatedAt(LocalDateTime.now());
            booking.setTicketId(generateTicketId("WSH"));
            last = workshopTicketBookingRepository.save(booking);
            generatedTicketIds.add(last.getTicketId());
        }

        return Map.of(
            "bookingId", last == null ? null : last.getId(),
                "message", "Workshop booked successfully",
            "ticketCount", ticketCount,
            "ticketEmails", ticketEmails,
            "ticketIds", generatedTicketIds
        );
    }

    @DeleteMapping("/events/bookings/{bookingId}")
    @Transactional
    public String cancelEventBookingByBookingId(@PathVariable Long bookingId, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        EventTicketBooking booking = eventTicketBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Event booking not found"));

        boolean isOwner = booking.getCustomer() != null && booking.getCustomer().getId().equals(customer.getId());
        boolean isRecipient = booking.getBookingEmail() != null
            && booking.getBookingEmail().equalsIgnoreCase(customer.getEmail());
        if (!isOwner && !isRecipient) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this booking");
        }

        eventTicketBookingRepository.delete(booking);
        return "Event booking canceled";
    }

    @DeleteMapping("/workshops/bookings/{bookingId}")
    @Transactional
    public String cancelWorkshopBookingByBookingId(@PathVariable Long bookingId, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        WorkshopTicketBooking booking = workshopTicketBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Workshop booking not found"));

        boolean isOwner = booking.getCustomer() != null && booking.getCustomer().getId().equals(customer.getId());
        boolean isRecipient = booking.getBookingEmail() != null
            && booking.getBookingEmail().equalsIgnoreCase(customer.getEmail());
        if (!isOwner && !isRecipient) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot cancel this booking");
        }

        workshopTicketBookingRepository.delete(booking);
        return "Workshop booking canceled";
    }

    @DeleteMapping("/workshops/{workshopId}/book")
    @Transactional
    public String cancelWorkshopBooking(@PathVariable Long workshopId, Authentication authentication) {
        Customer customer = (Customer) authentication.getPrincipal();
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));

        if (workshop.getCustomers() != null) {
            workshop.getCustomers().remove(customer);
        }
        if (customer.getWorkshopsBooked() != null) {
            customer.getWorkshopsBooked().remove(workshop);
        }

        workshopRepository.save(workshop);
        return "Workshop booking canceled";
    }
}