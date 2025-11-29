package com.dmdr.personal.portal.content.service.impl;

import com.dmdr.personal.portal.content.dto.UpdateHomePageRequest;
import com.dmdr.personal.portal.content.model.HomePage;
import com.dmdr.personal.portal.content.repository.HomePageRepository;
import com.dmdr.personal.portal.content.service.HomePageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomePageServiceImpl implements HomePageService {

    private final HomePageRepository homePageRepository;

    public HomePageServiceImpl(HomePageRepository homePageRepository) {
        this.homePageRepository = homePageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public HomePage getHomePage() {
        return homePageRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new IllegalStateException("HomePage not found. Please initialize the application."));
    }

    @Override
    @Transactional
    public HomePage updateHomePage(UpdateHomePageRequest request) {
        HomePage homePage = getHomePage();
        
        if (request.getWelcomeMessage() != null) {
            homePage.setWelcomeMessage(request.getWelcomeMessage());
        }
        if (request.getWelcomeMediaId() != null) {
            homePage.setWelcomeMediaId(request.getWelcomeMediaId());
        }
        if (request.getAboutMessage() != null) {
            homePage.setAboutMessage(request.getAboutMessage());
        }
        if (request.getAboutMediaId() != null) {
            homePage.setAboutMediaId(request.getAboutMediaId());
        }
        if (request.getEducationMessage() != null) {
            homePage.setEducationMessage(request.getEducationMessage());
        }
        if (request.getEducationMediaId() != null) {
            homePage.setEducationMediaId(request.getEducationMediaId());
        }
        
        return homePageRepository.save(homePage);
    }

    @Override
    @Transactional
    public void initializeDefaultHomePageIfNotExists() {
        if (homePageRepository.findFirstByOrderByCreatedAtAsc().isEmpty()) {
            HomePage defaultHomePage = new HomePage();
            defaultHomePage.setWelcomeMessage("Welcome to Personal Portal! We're excited to have you here.");
            defaultHomePage.setAboutMessage("This is a personal portal platform designed to help you manage your content, bookings, and more.");
            defaultHomePage.setEducationMessage("Our platform provides comprehensive tools for personal and professional growth.");
            homePageRepository.save(defaultHomePage);
        }
    }
}

