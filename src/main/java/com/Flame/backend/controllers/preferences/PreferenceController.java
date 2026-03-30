package com.Flame.backend.controllers.preferences;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Flame.backend.services.PreferenceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping
    public List<String> getAllPreferences() {
        return preferenceService.getAllPreferenceNames();
    }

    @PostMapping
    public Map<String, String> addCustomPreference(@RequestBody Map<String, String> payload) {
        String raw = payload.getOrDefault("name", "");
        String normalized = preferenceService.normalize(raw);
        preferenceService.ensureExists(normalized);
        return Map.of("name", normalized);
    }
}
