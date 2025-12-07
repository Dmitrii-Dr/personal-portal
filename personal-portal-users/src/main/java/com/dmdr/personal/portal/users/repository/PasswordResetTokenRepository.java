package com.dmdr.personal.portal.users.repository;

import com.dmdr.personal.portal.users.model.PasswordResetToken;
import com.dmdr.personal.portal.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a password reset token by its token string.
     *
     * @param token the token string to search for
     * @return an Optional containing the token if found, empty otherwise
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Deletes all password reset tokens associated with the given user.
     *
     * @param user the user whose tokens should be deleted
     */
    void deleteByUser(User user);
}

