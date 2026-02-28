package com.Flame.backend.DAO.event;

import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Provider;
import com.Flame.backend.entities.workshop.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByProvider(Provider provider);
}
