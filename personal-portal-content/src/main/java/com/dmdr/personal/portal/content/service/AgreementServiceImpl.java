package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.model.Agreement;
import com.dmdr.personal.portal.content.repository.AgreementRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;

    public AgreementServiceImpl(AgreementRepository agreementRepository) {
        this.agreementRepository = agreementRepository;
    }

    @Override
    @Transactional
    public Agreement createAgreement(Agreement agreement) {
        log.info("Creating new agreement with name: {}", agreement.getName());
        Agreement saved = agreementRepository.save(agreement);
        log.info("Agreement created successfully with id: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agreement> findById(UUID id) {
        log.debug("Finding agreement by id: {}", id);
        return agreementRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agreement> findBySlug(String slug) {
        log.debug("Finding agreement by slug: {}", slug);
        return agreementRepository.findBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agreement> findAll() {
        log.debug("Finding all agreements");
        return agreementRepository.findAll();
    }

    @Override
    @Transactional
    public Agreement updateAgreement(UUID id, Agreement agreement) {
        log.info("Updating agreement with id: {}", id);
        Agreement existing = agreementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found with id: " + id));

        boolean contentChanged = !existing.getName().equals(agreement.getName())
                || !existing.getContent().equals(agreement.getContent());
        boolean slugChanged = !existing.getSlug().equals(agreement.getSlug());

        if (slugChanged && !contentChanged) {
            log.info("Updating only slug for agreement with id: {}", id);
            agreementRepository.updateSlug(id, agreement.getSlug());
            existing.setSlug(agreement.getSlug());
            return existing;
        }

        existing.setName(agreement.getName());
        existing.setContent(agreement.getContent());
        existing.setSlug(agreement.getSlug());

        Agreement updated = agreementRepository.save(existing);
        log.info("Agreement updated successfully with id: {}", updated.getId());
        return updated;
    }

    @Override
    @Transactional
    public void deleteAgreement(UUID id) {
        log.info("Deleting agreement with id: {}", id);
        if (!agreementRepository.existsById(id)) {
            throw new RuntimeException("Agreement not found with id: " + id);
        }
        agreementRepository.deleteById(id);
        log.info("Agreement deleted successfully with id: {}", id);
    }
}
