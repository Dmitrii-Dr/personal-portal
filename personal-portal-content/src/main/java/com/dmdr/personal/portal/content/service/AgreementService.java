package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.model.Agreement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementService {

    Agreement createAgreement(Agreement agreement);

    Optional<Agreement> findById(UUID id);

    Optional<Agreement> findBySlug(String slug);

    List<Agreement> findAll();

    Agreement updateAgreement(UUID id, Agreement agreement);

    void deleteAgreement(UUID id);

}
