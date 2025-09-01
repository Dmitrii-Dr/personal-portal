package com.dmdr.personal.portal.repository;

import com.dmdr.personal.portal.model.Booking;
import com.dmdr.personal.portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByClientOrderByCreatedAtDesc(User client);
}
