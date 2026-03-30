package com.Flame.backend.controllers.workshop;


import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.workshop.Workshop;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workshops")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopRepository workshopRepository;

    @GetMapping
    public List<Workshop> getAllWorkshops(
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
            return workshopRepository.findAll();
        }

        return workshopRepository.searchFiltered(normalizedCategory, normalizedLocation, fromDate, toDate);
    }

    @GetMapping("/{id}")
    public Workshop getWorkshop(@PathVariable Long id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));
    }
}