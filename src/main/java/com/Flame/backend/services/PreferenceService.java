package com.Flame.backend.services;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Flame.backend.DAO.preference.PreferenceRepository;
import com.Flame.backend.entities.preference.Preference;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    public List<String> splitAndNormalize(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(this::normalize)
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }

    @Transactional
    public void ensureExists(String rawPreference) {
        String normalized = normalize(rawPreference);
        if (normalized.isBlank()) {
            return;
        }

        preferenceRepository.findByName(normalized)
                .orElseGet(() -> preferenceRepository.save(Preference.builder().name(normalized).build()));
    }

    @Transactional
    public void ensureAllExist(List<String> preferences) {
        if (preferences == null) {
            return;
        }
        preferences.forEach(this::ensureExists);
    }

    @Transactional(readOnly = true)
    public List<String> getAllPreferenceNames() {
        return preferenceRepository.findAllByOrderByNameAsc().stream()
                .map(Preference::getName)
                .toList();
    }

    public String mergeCsvPreferences(String existingCsv, String incomingCsv) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(splitAndNormalize(existingCsv));
        merged.addAll(splitAndNormalize(incomingCsv));
        return merged.stream().collect(Collectors.joining(","));
    }
}
