package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.BookingSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSettingsRepository extends JpaRepository<BookingSettings, Long> {
	Optional<BookingSettings> findTopByOrderByIdAsc();

	/**
	 * Finds the top BookingSettings ordered by ID ascending.
	 * Throws IllegalStateException if no BookingSettings is found.
	 * 
	 * @return BookingSettings (never null)
	 * @throws IllegalStateException if no BookingSettings exists
	 */
	default BookingSettings mustFindTopByOrderByIdAsc() {
		return findTopByOrderByIdAsc()
			.orElseThrow(() -> new IllegalStateException(
				"BookingSettings not found. Please configure booking settings before using the application."));
	}
}


