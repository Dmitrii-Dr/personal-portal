package com.dmdr.personal.portal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pages")
public class PageController {

    @GetMapping("/about-me")
    public Map<String, Object> getAboutMe() {
        return Map.of(
            "slug", "about-me",
            "title", "About Me",
            "content", "Hello this is my personal portal.",
            "lastUpdated", "2025-01-01T00:00:00Z"
        );
    }
}
