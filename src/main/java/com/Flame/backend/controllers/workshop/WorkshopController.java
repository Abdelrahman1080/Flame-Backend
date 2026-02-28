package com.Flame.backend.controllers.workshop;


import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.workshop.Workshop;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workshops")
@RequiredArgsConstructor
public class WorkshopController {

    private final WorkshopRepository workshopRepository;

    @GetMapping
    public List<Workshop> getAllWorkshops() {
        return workshopRepository.findAll();
    }

    @GetMapping("/{id}")
    public Workshop getWorkshop(@PathVariable Long id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found"));
    }
}