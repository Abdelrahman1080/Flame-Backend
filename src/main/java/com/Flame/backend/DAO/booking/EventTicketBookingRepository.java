package com.Flame.backend.DAO.booking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Flame.backend.entities.booking.EventTicketBooking;
import com.Flame.backend.entities.user.Customer;

public interface EventTicketBookingRepository extends JpaRepository<EventTicketBooking, Long> {
    List<EventTicketBooking> findByCustomerOrderByCreatedAtDesc(Customer customer);

    @Query("""
            SELECT b
            FROM EventTicketBooking b
            WHERE b.customer = :customer
               OR LOWER(COALESCE(b.bookingEmail, '')) = LOWER(:email)
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    List<EventTicketBooking> findVisibleBookings(
            @Param("customer") Customer customer,
            @Param("email") String email
    );
}
