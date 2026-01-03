package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.SignedAgreement;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AgreementVerifier {
    /**
     * Verifies that all required agreements are signed and returns snapshots of the
     * current agreement versions.
     * 
     * @param signedAgreements map of agreement ID to signed status (true/false)
     * @return list of signed agreement snapshots to be persisted with the user
     * @throws IllegalArgumentException if validation fails
     */
    List<SignedAgreement> verifyAndGetSnapshots(Map<UUID, Boolean> signedAgreements);
}
