package com.dmdr.personal.portal.repository;

import com.dmdr.personal.portal.model.BookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingSlotRepository extends JpaRepository<BookingSlot, Long> {
    List<BookingSlot> findByBookedFalseOrderByStartTimeAsc();
}
