package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.core.security.SystemRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AlreadyAuthenticatedFilter alreadyAuthenticatedFilter;

    public WebSecurityConfig(CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AlreadyAuthenticatedFilter alreadyAuthenticatedFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.alreadyAuthenticatedFilter = alreadyAuthenticatedFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(alreadyAuthenticatedFilter, JwtAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - health check and authentication endpoints
                        .requestMatchers("/api/v*/public/**", "/api/v1/health", "/api/v1/auth/**").permitAll()
                        // Admin endpoints - require ADMIN role
                        .requestMatchers("/api/v*/admin/**").hasAuthority(SystemRole.ADMIN.getAuthority())
                        // All other endpoints require authentication (USER token)
                        .anyRequest().authenticated());

        return http.build();
    }
}
