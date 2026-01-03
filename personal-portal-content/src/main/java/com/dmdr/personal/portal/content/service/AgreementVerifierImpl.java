package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.model.Agreement;
import com.dmdr.personal.portal.content.repository.AgreementRepository;
import com.dmdr.personal.portal.users.model.SignedAgreement;
import com.dmdr.personal.portal.users.service.AgreementVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementVerifierImpl implements AgreementVerifier {

    private final AgreementRepository agreementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SignedAgreement> verifyAndGetSnapshots(Map<UUID, Boolean> signedAgreements) {
        if (signedAgreements == null || signedAgreements.isEmpty()) {
            // Check if there are any agreements in the DB. If so, they must be signed.
            // If DB has no agreements, then empty map is fine.
            long count = agreementRepository.count();
            if (count > 0) {
                throw new IllegalArgumentException("All agreements must be signed");
            }
            return new ArrayList<>();
        }

        // 1. Fetch all agreements from DB
        List<Agreement> allAgreements = agreementRepository.findAll();

        if (allAgreements.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Validate that all DB agreements are present in the request and true
        List<UUID> missingOrUnsigned = allAgreements.stream()
                .filter(a -> !Boolean.TRUE.equals(signedAgreements.get(a.getId())))
                .map(Agreement::getId)
                .collect(Collectors.toList());

        if (!missingOrUnsigned.isEmpty()) {
            throw new IllegalArgumentException("Agreements not signed: " + missingOrUnsigned);
        }

        // 3. Create snapshots
        return allAgreements.stream()
                .map(this::createSnapshot)
                .collect(Collectors.toList());
    }

    private SignedAgreement createSnapshot(Agreement agreement) {
        return SignedAgreement.builder()
                .agreementId(agreement.getId())
                .name(agreement.getName())
                .content(agreement.getContent())
                .version(agreement.getVersion())
                .slug(agreement.getSlug())
                .signedAt(OffsetDateTime.now())
                .build();
    }
}
