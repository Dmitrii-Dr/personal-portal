package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.content.dto.UpdateHomePageRequest;
import com.dmdr.personal.portal.content.model.HomePage;
import com.dmdr.personal.portal.content.service.HomePageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/home")
public class AdminInfoController {

    private final HomePageService homePageService;

    public AdminInfoController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @PutMapping
    public ResponseEntity<HomePage> updateHomePage(@Valid @RequestBody UpdateHomePageRequest request) {
        HomePage updated = homePageService.updateHomePage(request);
        return ResponseEntity.ok(updated);
    }
}

