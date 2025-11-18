package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.Booking;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
	List<Booking> findByClientId(UUID clientId);
}

