package com.Flame.backend.controllers.users;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.Flame.backend.DAO.users.CustomerRepository;
import com.Flame.backend.entities.user.Customer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Flame.backend.DAO.event.EventRepository;
import com.Flame.backend.DAO.users.ProviderRepository;
import com.Flame.backend.DAO.workshop.WorkshopRepository;
import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.workshop.Workshop;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pages")
@RequiredArgsConstructor
public class CompanyPageController {

    private final CustomerRepository customerRepository;
    private final EventRepository eventRepository;
    private final WorkshopRepository workshopRepository;

    @GetMapping
    public List<Map<String, Object>> getAllCompanyPages() {
        return customerRepository.findAll().stream()
                                .map(provider -> {
                                        Map<String, Object> row = new LinkedHashMap<>();
                                        row.put("providerId", provider.getId());
                                        row.put("companyName", provider.getCompanyName() == null ? "Company" : provider.getCompanyName());
                                      //  row.put("companyLogoUrl", provider.getCompanyLogoUrl());
                                        //row.put("companyTagline", provider.getCompanyTagline());
                                        //row.put("companyLocation", provider.getCompanyLocation());
                                        return row;
                                })
                .toList();
    }

    @GetMapping("/{providerId}")
    public Map<String, Object> getCompanyPage(@PathVariable Integer providerId) {
        Customer customer = customerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Company page not found"));

        List<Event> events = eventRepository.findByProvider(customer);
        List<Workshop> workshops = workshopRepository.findByProvider(customer);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("providerId", customer.getId());
        response.put("companyName", customer.getCompanyName() == null ? "Company" : customer.getCompanyName());
       // response.put("companyLogoUrl", provider.getCompanyLogoUrl());
        //response.put("companyTagline", provider.getCompanyTagline());
        //response.put("companyDescription", provider.getCompanyDescription());
        //response.put("companyWebsite", provider.getCompanyWebsite());
        //response.put("companyLocation", provider.getCompanyLocation());
        response.put("events", events);
        response.put("workshops", workshops);
        return response;
    }
}
