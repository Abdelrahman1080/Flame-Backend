package com.Flame.backend.controllers.events;


import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.entities.event.Event;

import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventRepository eventRepository;

    @GetMapping
    public List<Event> getAllEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        String normalizedCategory = category == null ? null : category.trim();
        String normalizedLocation = location == null ? null : location.trim();

        if ((normalizedCategory == null || normalizedCategory.isEmpty())
                && (normalizedLocation == null || normalizedLocation.isEmpty())
                && fromDate == null
                && toDate == null) {
            return eventRepository.findAll();
        }

        Specification<Event> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (normalizedCategory != null && !normalizedCategory.isEmpty()) {
            String categoryFilter = normalizedCategory.toLowerCase();
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("category"), "")),
                    "%" + categoryFilter + "%"
                )
            );
        }

        if (normalizedLocation != null && !normalizedLocation.isEmpty()) {
            String locationFilter = normalizedLocation.toLowerCase();
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                    criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("location"), "")),
                    "%" + locationFilter + "%"
                )
            );
        }

        if (fromDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), fromDate),
                    criteriaBuilder.and(
                        criteriaBuilder.isNull(root.get("endDate")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), fromDate)
                    )
                )
            );
        }

        if (toDate != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), toDate)
            );
        }

        return eventRepository.findAll(specification, Sort.by(Sort.Order.asc("startDate"), Sort.Order.desc("id")));
    }

    @GetMapping("/{id}")
    public Event getEvent(@PathVariable Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
    }
}