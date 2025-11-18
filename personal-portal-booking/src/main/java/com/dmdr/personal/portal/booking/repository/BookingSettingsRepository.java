package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.BookingSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSettingsRepository extends JpaRepository<BookingSettings, Long> {
	Optional<BookingSettings> findTopByOrderByIdAsc();
}


