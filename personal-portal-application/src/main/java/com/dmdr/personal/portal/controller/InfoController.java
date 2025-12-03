package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.content.dto.ContactDto;
import com.dmdr.personal.portal.content.dto.HomePageResponse;
import com.dmdr.personal.portal.content.model.HomePage;
import com.dmdr.personal.portal.content.service.HomePageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/public")
public class InfoController {

    private final HomePageService homePageService;

    public InfoController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @GetMapping("/welcome")
    public ResponseEntity<HomePageResponse> getWelcomeInfo() {
        HomePage homePage = homePageService.getHomePage();
        HomePageResponse response = toHomePageResponse(homePage);
        return ResponseEntity.ok(response);
    }

    private HomePageResponse toHomePageResponse(HomePage homePage) {
        HomePageResponse response = new HomePageResponse();
        response.setWelcomeMessage(homePage.getWelcomeMessage());
        response.setWelcomeMediaId(homePage.getWelcomeMediaId());
        response.setWelcomeArticleIds(homePage.getWelcomeArticleIds());
        response.setAboutMessage(homePage.getAboutMessage());
        response.setAboutMediaId(homePage.getAboutMediaId());
        response.setEducationMessage(homePage.getEducationMessage());
        response.setEducationMediaId(homePage.getEducationMediaId());
        response.setReviewMessage(homePage.getReviewMessage());
        response.setReviewMediaIds(homePage.getReviewMediaIds());
        if (homePage.getContact() != null) {
            List<ContactDto> contactDtos = homePage.getContact().stream()
                    .map(contact -> new ContactDto(contact.getPlatform(), contact.getValue(), contact.getDescription()))
                    .toList();
            response.setContact(contactDtos);
        }
        return response;
    }

    @GetMapping("/more-about-me")
    public ResponseEntity<Map<String, String>> getMoreAboutMe() {
        // Generate exactly 1000 characters of stub text
        String baseText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. ";
        StringBuilder stubMessage = new StringBuilder();
        while (stubMessage.length() < 1000) {
            stubMessage.append(baseText);
        }
        String message = stubMessage.substring(0, 1000);
        
        Map<String, String> response = Map.of("message", message);
        return ResponseEntity.ok(response);
    }
}

