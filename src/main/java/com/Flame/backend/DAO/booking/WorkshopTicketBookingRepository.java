package com.Flame.backend.DAO.booking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Flame.backend.entities.booking.WorkshopTicketBooking;
import com.Flame.backend.entities.user.Customer;

public interface WorkshopTicketBookingRepository extends JpaRepository<WorkshopTicketBooking, Long> {
    List<WorkshopTicketBooking> findByCustomerOrderByCreatedAtDesc(Customer customer);

    @Query("""
            SELECT b
            FROM WorkshopTicketBooking b
            WHERE b.customer = :customer
               OR LOWER(COALESCE(b.bookingEmail, '')) = LOWER(:email)
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    List<WorkshopTicketBooking> findVisibleBookings(
            @Param("customer") Customer customer,
            @Param("email") String email
    );
}
