package com.dmdr.personal.portal.service.impl;

import com.dmdr.personal.portal.dto.RegistrationDto;
import com.dmdr.personal.portal.model.User;
import com.dmdr.personal.portal.repository.UserRepository;
import com.dmdr.personal.portal.service.EmailService;
import com.dmdr.personal.portal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public User register(RegistrationDto dto) {
        // TODO: Add validations, duplicate checks
        User user = User.builder()
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role("ROLE_USER")
                .createdAt(Instant.now())
                .build();
        user = userRepository.save(user);
        emailService.sendWelcomeEmail(user);
        return user;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
