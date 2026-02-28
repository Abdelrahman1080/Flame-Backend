package com.Flame.backend.DAO.users;

import com.Flame.backend.entities.user.Customer;
import com.Flame.backend.entities.user.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
}