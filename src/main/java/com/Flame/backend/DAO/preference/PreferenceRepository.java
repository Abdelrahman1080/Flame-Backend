package com.Flame.backend.DAO.preference;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Flame.backend.entities.preference.Preference;

public interface PreferenceRepository extends JpaRepository<Preference, Long> {
    Optional<Preference> findByName(String name);
    List<Preference> findAllByOrderByNameAsc();
}
