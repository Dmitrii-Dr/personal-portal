package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.model.HomePage;

public interface HomePageService {
    
    HomePage getHomePage();
    
    HomePage updateHomePage(com.dmdr.personal.portal.content.dto.UpdateHomePageRequest request);
    
    void initializeDefaultHomePageIfNotExists();
}

