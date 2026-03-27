package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import com.dmdr.personal.portal.service.exception.PortalErrorCode;
import java.security.SecureRandom;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 72;

    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_WHITESPACE = Pattern.compile(".*\\s.*");

    public void validatePasswordRequirements(String password) {
        if (password == null || password.isBlank()) {
            throw new PersonalPortalRuntimeException(PortalErrorCode.PASSWORD_REQUIREMENTS_NOT_MET);
        }

        int length = password.length();
        if (length < PASSWORD_MIN_LENGTH || length > PASSWORD_MAX_LENGTH) {
            throw new PersonalPortalRuntimeException(PortalErrorCode.PASSWORD_REQUIREMENTS_NOT_MET);
        }

        if (HAS_WHITESPACE.matcher(password).matches()) {
            throw new PersonalPortalRuntimeException(PortalErrorCode.PASSWORD_REQUIREMENTS_NOT_MET);
        }

        if (!HAS_UPPER.matcher(password).matches()
                || !HAS_LOWER.matcher(password).matches()
                || !HAS_DIGIT.matcher(password).matches()) {
            throw new PersonalPortalRuntimeException(PortalErrorCode.PASSWORD_REQUIREMENTS_NOT_MET);
        }
    }

    /**
     * Generates a secure random password with a mix of uppercase, lowercase,
     * digits, and special characters for admin-created users.
     */
    public String generateRandomPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String allChars = uppercase + lowercase + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(16);

        // Ensure at least one character from each category
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill the rest randomly
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password to avoid predictable pattern
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }
}

