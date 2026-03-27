package com.dmdr.personal.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Provides a UserDetailsService for the embedded SBA client so it can authenticate
 * via HTTP Basic when registering with the admin server (same application instance).
 * Credentials are taken from spring.boot.admin.client.username/password.
 */
@Configuration
public class SbaClientAuthConfig {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Bean
    public UserDetailsService sbaClientUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${spring.boot.admin.client.username}") String username,
            @Value("${spring.boot.admin.client.password}") String password) {
        var user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .authorities(ROLE_ADMIN)
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
