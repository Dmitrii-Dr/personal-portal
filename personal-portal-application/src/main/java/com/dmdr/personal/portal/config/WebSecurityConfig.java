package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.core.security.SystemRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AccountActivationFilter accountActivationFilter;
    private final HomePageActiveFilter homePageActiveFilter;
    private final AlreadyAuthenticatedFilter alreadyAuthenticatedFilter;
    private final CsrfTokenRepository csrfTokenRepository;
    private final CsrfTokenRequestHandler csrfTokenRequestHandler;
    private final RequestMatcher csrfRequestMatcher;

    public WebSecurityConfig(CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AccountActivationFilter accountActivationFilter,
            HomePageActiveFilter homePageActiveFilter,
            AlreadyAuthenticatedFilter alreadyAuthenticatedFilter,
            CsrfTokenRepository csrfTokenRepository,
            CsrfTokenRequestHandler csrfTokenRequestHandler,
            RequestMatcher csrfRequestMatcher) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.accountActivationFilter = accountActivationFilter;
        this.homePageActiveFilter = homePageActiveFilter;
        this.alreadyAuthenticatedFilter = alreadyAuthenticatedFilter;
        this.csrfTokenRepository = csrfTokenRepository;
        this.csrfTokenRequestHandler = csrfTokenRequestHandler;
        this.csrfRequestMatcher = csrfRequestMatcher;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                        .requireCsrfProtectionMatcher(csrfRequestMatcher))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(homePageActiveFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(accountActivationFilter, HomePageActiveFilter.class)
                .addFilterAfter(alreadyAuthenticatedFilter, JwtAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - health check and authentication endpoints
                        .requestMatchers("/api/v*/public/**", "/api/v1/health", "/api/v1/auth/**",
                                "/actuator/health/**")
                        .permitAll()
                        // Admin endpoints - require ADMIN role
                        .requestMatchers("/api/v*/admin/**").hasAuthority(SystemRole.ADMIN.getAuthority())
                        // All other endpoints require authentication (USER token)
                        .anyRequest().authenticated());

        return http.build();
    }
}
