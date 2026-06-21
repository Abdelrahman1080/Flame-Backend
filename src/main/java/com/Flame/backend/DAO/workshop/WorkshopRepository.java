package com.Flame.backend.DAO.workshop;

import com.Flame.backend.entities.event.Event;
import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.workshop.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {

    List<Workshop> findByProvider(Customer customer);

}
