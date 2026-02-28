package com.dmdr.personal.portal.users.repository;

import com.dmdr.personal.portal.users.model.AccountVerificationCode;
import com.dmdr.personal.portal.users.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountVerificationCodeRepository extends JpaRepository<AccountVerificationCode, Long> {

    Optional<AccountVerificationCode> findByUser(User user);

    void deleteByUser(User user);
}
