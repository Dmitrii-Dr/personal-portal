package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.users.model.AccountVerificationCode;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.repository.AccountVerificationCodeRepository;
import com.dmdr.personal.portal.users.repository.UserRepository;
import com.dmdr.personal.portal.users.service.AccountVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
@Transactional
@Slf4j
public class AccountVerificationServiceImpl implements AccountVerificationService {

    private static final int CODE_UPPER_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();

    private final AccountVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final int codeExpiryMinutes;
    private final int maxAttempts;

    public AccountVerificationServiceImpl(AccountVerificationCodeRepository verificationCodeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${account.verification.code.expiry-minutes:10}") int codeExpiryMinutes,
            @Value("${account.verification.code.max-attempts:5}") int maxAttempts) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.codeExpiryMinutes = codeExpiryMinutes;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public void issueVerificationCode(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.isActive()) {
            return;
        }

        String rawCode = generateCode();
        String codeHash = passwordEncoder.encode(rawCode);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(codeExpiryMinutes);

        AccountVerificationCode verificationCode = verificationCodeRepository.findByUser(user)
                .orElseGet(AccountVerificationCode::new);
        verificationCode.setUser(user);
        verificationCode.setCodeHash(codeHash);
        verificationCode.setExpiresAt(expiresAt);
        verificationCode.setFailedAttempts(0);
        verificationCodeRepository.save(verificationCode);

        try {
            emailService.sendAccountVerificationCodeEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    rawCode,
                    codeExpiryMinutes);
            log.info("Account verification code sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send account verification code to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    @Override
    public void verifyCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));

        if (user.isActive()) {
            return;
        }

        AccountVerificationCode verificationCode = verificationCodeRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));

        if (verificationCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
            verificationCodeRepository.delete(verificationCode);
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        if (!passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            int attempts = verificationCode.getFailedAttempts() + 1;
            verificationCode.setFailedAttempts(attempts);

            if (attempts >= maxAttempts) {
                verificationCodeRepository.delete(verificationCode);
            } else {
                verificationCodeRepository.save(verificationCode);
            }

            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        user.setActive(true);
        userRepository.save(user);
        verificationCodeRepository.delete(verificationCode);
        log.info("User account activated: {}", user.getEmail());
    }

    private String generateCode() {
        int value = secureRandom.nextInt(CODE_UPPER_BOUND);
        return String.format("%06d", value);
    }
}
