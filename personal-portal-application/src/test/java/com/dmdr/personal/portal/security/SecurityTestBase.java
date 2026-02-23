package com.dmdr.personal.portal.security;

import com.dmdr.personal.portal.service.JwtService;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;
import com.dmdr.personal.portal.users.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class SecurityTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserService userService;

    @MockBean
    protected com.dmdr.personal.portal.config.AdminUserInitializer adminUserInitializer;

    @MockBean
    protected com.dmdr.personal.portal.booking.config.BookingSettingsInitializer bookingSettingsInitializer;

    @MockBean
    protected com.dmdr.personal.portal.config.HomePageInitializer homePageInitializer;

    @MockBean
    protected com.dmdr.personal.portal.booking.service.impl.AvailabilityOverrideArchiveScheduler availabilityOverrideArchiveScheduler;

    @MockBean
    protected com.dmdr.personal.portal.booking.service.impl.AvailabilityRuleArchiveScheduler availabilityRuleArchiveScheduler;

    protected String generateUserToken() {
        return jwtService.generateToken(UUID.randomUUID(), Set.of("ROLE_USER"), UUID.randomUUID());
    }

    protected String generateAdminToken() {
        return jwtService.generateToken(UUID.randomUUID(), Set.of("ROLE_ADMIN"), UUID.randomUUID());
    }

    protected String generateUserAndAdminToken() {
        return jwtService.generateToken(UUID.randomUUID(), Set.of("ROLE_USER", "ROLE_ADMIN"), UUID.randomUUID());
    }

    protected String generateInvalidToken() {
        return "invalid.token.here";
    }

    protected void createTestUser(String email, String password) {
        try {
            CreateUserRequest request = new CreateUserRequest(email, password, "John", "Doe", null, null);
            userService.createUser(request);
        } catch (Exception e) {
            // User might already exist, ignore
        }
    }
}
