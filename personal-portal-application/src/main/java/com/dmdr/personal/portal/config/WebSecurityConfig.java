package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.core.security.SystemRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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

    /**
     * SBA/actuator chain: /admin/sba/** (UI + registration API) and /actuator/**.
     * - Form login: SBA UI POSTs to /admin/sba/login; Spring Security handles it (same credentials as SBA client).
     * - HTTP Basic: still used for SBA client registration and optional browser auth.
     * - Sessions used for /admin/sba so after form login the UI stays authenticated.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain sbaActuatorFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/sba/**", "/actuator/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(basic -> basic.realmName("Spring Boot Admin"))
                .formLogin(form -> form
                        .loginPage("/admin/sba/login")
                        .loginProcessingUrl("/admin/sba/login")
                        .defaultSuccessUrl("/admin/sba/", true)
                        .permitAll())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/admin/sba/**", "/actuator/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/sba", "/admin/sba/", "/admin/sba/login", "/admin/sba/assets/**").permitAll()
                        .anyRequest().hasAuthority(SystemRole.ADMIN.getAuthority()));

        return http.build();
    }

    /**
     * Main chain: all other endpoints. JWT only, no HTTP Basic.
     */
    @Bean
    @Order(2)
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
                                "/actuator/health", "/actuator/health/**")
                        .permitAll()
                        // Admin endpoints - require ADMIN role (API admin; /admin/sba and /actuator handled by sbaActuatorFilterChain)
                        .requestMatchers("/api/v*/admin/**").hasAuthority(SystemRole.ADMIN.getAuthority())
                        // All other endpoints require authentication (USER token)
                        .anyRequest().authenticated());

        return http.build();
    }
}
