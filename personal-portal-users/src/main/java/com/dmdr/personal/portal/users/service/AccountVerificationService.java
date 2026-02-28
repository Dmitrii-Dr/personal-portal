package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.User;

public interface AccountVerificationService {

    void issueVerificationCode(User user);

    void verifyCode(User user, String code);
}
