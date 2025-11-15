package com.dmdr.personal.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        String trimmedOrigins = allowedOrigins.trim();
        boolean isWildcard = "*".equals(trimmedOrigins);
        
        if (isWildcard) {
            // Wildcard mode: allow all origins but disable credentials
            // This is useful for development but not recommended for production
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
            configuration.setAllowCredentials(false);
        } else {
            // Specific origins mode: parse comma-separated list and enable credentials
            // This allows JWT tokens in Authorization headers
            List<String> originList = Stream.of(trimmedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            
            configuration.setAllowedOrigins(originList);
            configuration.setAllowCredentials(true);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

