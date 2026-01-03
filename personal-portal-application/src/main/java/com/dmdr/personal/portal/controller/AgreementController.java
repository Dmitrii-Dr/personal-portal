package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.content.dto.AgreementDictionaryItem;
import com.dmdr.personal.portal.content.dto.AgreementResponse;
import com.dmdr.personal.portal.content.model.Agreement;
import com.dmdr.personal.portal.content.service.AgreementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/agreements")
@RequiredArgsConstructor
@Slf4j
public class AgreementController {

    private final AgreementService agreementService;

    @GetMapping("/dictionary")
    public ResponseEntity<List<AgreementDictionaryItem>> getAgreementDictionary() {
        log.debug("Getting agreement dictionary");
        List<Agreement> agreements = agreementService.findAll();

        List<AgreementDictionaryItem> dictionary = agreements.stream()
                .map(agreement -> new AgreementDictionaryItem(
                        agreement.getId(),
                        agreement.getName(),
                        agreement.getSlug()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dictionary);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgreementResponse> getAgreementById(@PathVariable("id") UUID id) {
        log.debug("Getting agreement by id: {}", id);
        Agreement agreement = agreementService.findById(id)
                .orElseThrow(() -> new RuntimeException("Agreement not found with id: " + id));
        return ResponseEntity.ok(toResponse(agreement));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<AgreementResponse> getAgreementBySlug(@PathVariable("slug") String slug) {
        log.debug("Getting agreement by slug: {}", slug);
        Agreement agreement = agreementService.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Agreement not found with slug: " + slug));
        return ResponseEntity.ok(toResponse(agreement));
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
