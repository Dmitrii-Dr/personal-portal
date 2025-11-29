package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.Booking;
import com.dmdr.personal.portal.booking.model.BookingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
	List<Booking> findByClientId(UUID clientId);

	@Query("SELECT b FROM Booking b WHERE b.status IN :statuses " +
		"AND b.startTime < :dayEnd AND b.endTime > :dayStart")
	List<Booking> findBookingsByStatusAndTimeRange(
		@Param("statuses") List<BookingStatus> statuses,
		@Param("dayStart") Instant dayStart,
		@Param("dayEnd") Instant dayEnd
	);

	List<Booking> findByStatusOrderByStartTimeAsc(BookingStatus status);
}

