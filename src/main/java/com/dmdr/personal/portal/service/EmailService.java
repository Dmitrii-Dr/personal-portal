package com.dmdr.personal.portal.service;

import com.dmdr.personal.portal.model.Booking;
import com.dmdr.personal.portal.model.User;

public interface EmailService {
    void sendWelcomeEmail(User user);
    void sendBookingConfirmation(Booking booking);
}
