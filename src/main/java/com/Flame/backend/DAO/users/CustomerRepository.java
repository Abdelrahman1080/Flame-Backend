package com.Flame.backend.DAO.users;

import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}