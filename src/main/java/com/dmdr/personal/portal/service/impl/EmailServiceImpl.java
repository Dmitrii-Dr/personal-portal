package com.dmdr.personal.portal.service.impl;

import com.dmdr.personal.portal.model.Booking;
import com.dmdr.personal.portal.model.User;
import com.dmdr.personal.portal.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendWelcomeEmail(User user) {
        // Context context = new Context();
        // context.setVariable("user", user);
        // String htmlContent = templateEngine.process("email/welcome", context);
        // sendEmail(user.getEmail(), "Добро пожаловать! Давайте разбираться! ", htmlContent);
    }

    @Override
    public void sendBookingConfirmation(Booking booking) {
        // Context context = new Context();
        // context.setVariable("booking", booking);
        // String htmlContent = templateEngine.process("email/booking-confirmation", context);
        // sendEmail(booking.getClient().getEmail(), "Booking Confirmed", htmlContent);
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        // try {
        //     MimeMessage message = mailSender.createMimeMessage();
        //     MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        //     helper.setTo(to);
        //     helper.setSubject(subject);
        //     helper.setText(htmlContent, true);
        //     mailSender.send(message);
        // } catch (MessagingException e) {
        //     // Log error, but don't throw to avoid breaking flow
        //     System.err.println("Failed to send email: " + e.getMessage());
        // }
    }
}
