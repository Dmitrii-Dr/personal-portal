package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.content.dto.UpdateHomePageRequest;
import com.dmdr.personal.portal.content.model.HomePage;
import com.dmdr.personal.portal.content.service.HomePageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/home")
@Slf4j
public class AdminHomeController {

    private final HomePageService homePageService;

    public AdminHomeController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @PutMapping
    public ResponseEntity<HomePage> updateHomePage(@Valid @RequestBody UpdateHomePageRequest request) {
        int welcomeArticleCount = request.getWelcomeArticleIds() != null ? request.getWelcomeArticleIds().size() : 0;
        int contactCount = request.getContact() != null ? request.getContact().size() : 0;
        String ctx = "isActive=" + request.getIsActive()
            + " welcomeArticleIdsSize=" + welcomeArticleCount
            + " contactSize=" + contactCount;
        log.info("BEGIN updateHomePage {}", ctx);
        try {
            HomePage updated = homePageService.updateHomePage(request);
            return ResponseEntity.ok(updated);
        } finally {
            log.info("END updateHomePage {}", ctx);
        }
    }
}

