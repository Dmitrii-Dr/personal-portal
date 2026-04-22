package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.content.dto.AgreementResponse;
import com.dmdr.personal.portal.content.dto.CreateAgreementRequest;
import com.dmdr.personal.portal.content.dto.UpdateAgreementRequest;
import com.dmdr.personal.portal.content.model.Agreement;
import com.dmdr.personal.portal.content.service.AgreementService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<List<AgreementResponse>> getAllAgreements(HttpServletRequest httpRequest) {
        String ctx = AdminApiLogSupport.http(httpRequest);
        log.info("BEGIN getAllAgreements {}", ctx);
        try {
            List<Agreement> agreements = agreementService.findAll();
            List<AgreementResponse> responses = agreements.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } finally {
            log.info("END getAllAgreements {}", ctx);
        }
    }

    @PostMapping
    public ResponseEntity<AgreementResponse> createAgreement(@Valid @RequestBody CreateAgreementRequest request) {
        int nameLen = request.getName() != null ? request.getName().length() : 0;
        int slugLen = request.getSlug() != null ? request.getSlug().length() : 0;
        int contentLen = request.getContent() != null ? request.getContent().length() : 0;
        String ctx = "nameLength=" + nameLen + " slugLength=" + slugLen + " contentLength=" + contentLen;
        log.info("BEGIN createAgreement {}", ctx);
        try {
            Agreement agreement = new Agreement();
            agreement.setName(request.getName());
            agreement.setContent(request.getContent());
            agreement.setSlug(request.getSlug());

            Agreement created = agreementService.createAgreement(agreement);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
        } finally {
            log.info("END createAgreement {}", ctx);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgreementResponse> updateAgreement(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateAgreementRequest request) {
        int nameLen = request.getName() != null ? request.getName().length() : 0;
        int slugLen = request.getSlug() != null ? request.getSlug().length() : 0;
        String ctx = "agreementId=" + id + " nameLength=" + nameLen + " slugLength=" + slugLen;
        log.info("BEGIN updateAgreement {}", ctx);
        try {
            Agreement agreement = new Agreement();
            agreement.setName(request.getName());
            agreement.setContent(request.getContent());
            agreement.setSlug(request.getSlug());

            Agreement updated = agreementService.updateAgreement(id, agreement);
            return ResponseEntity.ok(toResponse(updated));
        } finally {
            log.info("END updateAgreement {}", ctx);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgreement(@PathVariable("id") UUID id) {
        String ctx = "agreementId=" + id;
        log.info("BEGIN deleteAgreement {}", ctx);
        try {
            agreementService.deleteAgreement(id);
            return ResponseEntity.noContent().build();
        } finally {
            log.info("END deleteAgreement {}", ctx);
        }
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
