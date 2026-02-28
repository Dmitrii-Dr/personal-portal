package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import com.dmdr.personal.portal.service.exception.PortalErrorCode;
import com.dmdr.personal.portal.users.dto.RequestVerificationCodeRequest;
import com.dmdr.personal.portal.users.dto.VerifyAccountRequest;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.AccountVerificationService;
import com.dmdr.personal.portal.users.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/account/activation")
@Slf4j
public class UserAccountActivationController {

    private final UserService userService;
    private final AccountVerificationService accountVerificationService;

    public UserAccountActivationController(UserService userService,
            AccountVerificationService accountVerificationService) {
        this.userService = userService;
        this.accountVerificationService = accountVerificationService;
    }

    @PostMapping("/verification")
    public ResponseEntity<Void> verifyAccount(@Valid @RequestBody VerifyAccountRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        try {
            accountVerificationService.verifyCode(user, request.getCode());
        } catch (IllegalArgumentException e) {
            throw new PersonalPortalRuntimeException(PortalErrorCode.INVALID_OR_EXPIRED_VERIFICATION_CODE);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/code")
    public ResponseEntity<Void> requestVerificationCode(@Valid @RequestBody RequestVerificationCodeRequest request) {
        try {
            User user = userService.findByEmail(request.getEmail()).orElse(null);
            if (user == null) {
                log.error("Verification code was requested but no user exists with email {}", request.getEmail());
                // Return success for security reasons.
                return ResponseEntity.noContent().build();
            }
            accountVerificationService.issueVerificationCode(user);
        } catch (IllegalStateException ignored) {
            // Security: same response to avoid revealing account state.
        }
        return ResponseEntity.noContent().build();
    }
}
