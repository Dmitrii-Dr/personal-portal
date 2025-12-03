package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.content.service.HomePageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@Slf4j
public class HomePageInitializer implements CommandLineRunner {

    private final HomePageService homePageService;

    public HomePageInitializer(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @Override
    public void run(String... args) {
        try {
            homePageService.initializeDefaultHomePageIfNotExists();
            log.info("HomePage initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize HomePage", e);
        }
    }
}

