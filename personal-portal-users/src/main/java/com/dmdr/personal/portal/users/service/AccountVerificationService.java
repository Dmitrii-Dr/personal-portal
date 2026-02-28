package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.User;

public interface AccountVerificationService {

    void issueVerificationCode(User user);

    void verifyCode(String email, String code);
}
