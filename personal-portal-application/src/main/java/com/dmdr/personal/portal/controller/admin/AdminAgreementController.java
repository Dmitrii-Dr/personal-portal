package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.content.dto.AgreementResponse;
import com.dmdr.personal.portal.content.dto.CreateAgreementRequest;
import com.dmdr.personal.portal.content.dto.UpdateAgreementRequest;
import com.dmdr.personal.portal.content.model.Agreement;
import com.dmdr.personal.portal.content.service.AgreementService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/agreements")
@Slf4j
public class AdminAgreementController {

    private final AgreementService agreementService;

    public AdminAgreementController(AgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @GetMapping
    public ResponseEntity<List<AgreementResponse>> getAllAgreements() {
        log.debug("Getting all agreements");
        List<Agreement> agreements = agreementService.findAll();
        List<AgreementResponse> responses = agreements.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<AgreementResponse> createAgreement(@Valid @RequestBody CreateAgreementRequest request) {
        log.info("Creating new agreement with name: {}", request.getName());

        Agreement agreement = new Agreement();
        agreement.setName(request.getName());
        agreement.setContent(request.getContent());
        agreement.setSlug(request.getSlug());

        Agreement created = agreementService.createAgreement(agreement);
        log.info("Agreement created successfully with id: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgreementResponse> updateAgreement(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAgreementRequest request) {
        log.info("Updating agreement with id: {}", id);

        Agreement agreement = new Agreement();
        agreement.setName(request.getName());
        agreement.setContent(request.getContent());
        agreement.setSlug(request.getSlug());

        Agreement updated = agreementService.updateAgreement(id, agreement);
        log.info("Agreement updated successfully with id: {}", updated.getId());

        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgreement(@PathVariable("id") UUID id) {
        log.info("Deleting agreement with id: {}", id);
        agreementService.deleteAgreement(id);
        log.info("Agreement deleted successfully with id: {}", id);
        return ResponseEntity.noContent().build();
    }

    private AgreementResponse toResponse(Agreement agreement) {
        AgreementResponse response = new AgreementResponse();
        response.setId(agreement.getId());
        response.setName(agreement.getName());
        response.setContent(agreement.getContent());
        response.setSlug(agreement.getSlug());
        response.setCreatedAt(agreement.getCreatedAt());
        response.setUpdatedAt(agreement.getUpdatedAt());
        response.setVersion(agreement.getVersion());
        return response;
    }
}
