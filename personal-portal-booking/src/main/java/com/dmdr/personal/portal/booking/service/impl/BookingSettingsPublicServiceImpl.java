package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingSettingsResponse;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.repository.BookingSettingsRepository;
import com.dmdr.personal.portal.booking.service.BookingSettingsPublicService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingSettingsPublicServiceImpl implements BookingSettingsPublicService {

	private final BookingSettingsRepository bookingSettingsRepository;

	public BookingSettingsPublicServiceImpl(BookingSettingsRepository bookingSettingsRepository) {
		this.bookingSettingsRepository = bookingSettingsRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public BookingSettingsResponse getIntervals() {
		BookingSettings settings = bookingSettingsRepository.mustFindTopByOrderByIdAsc();
		BookingSettingsResponse response = new BookingSettingsResponse();
		response.setBookingCancelationInterval(settings.getBookingCancelationInterval());
		response.setBookingUpdatingInterval(settings.getBookingUpdatingInterval());
		return response;
	}
}
