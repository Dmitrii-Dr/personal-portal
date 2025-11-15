package com.dmdr.personal.portal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Admin Dashboard Security Tests")
class AdminDashboardSecurityTest extends SecurityTestBase {

    private static final String DASHBOARD_URL = "/api/v1/admin/dashboard";

    @Test
    @DisplayName("Should allow access to dashboard with ADMIN token")
    void shouldAllowAccessWithAdminToken() throws Exception {
        String adminToken = generateAdminToken();

        mockMvc.perform(MockMvcRequestBuilders.get(DASHBOARD_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access to dashboard without token")
    void shouldDenyAccessWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(DASHBOARD_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny access to dashboard with USER token")
    void shouldDenyAccessWithUserToken() throws Exception {
        String userToken = generateUserToken();

        mockMvc.perform(MockMvcRequestBuilders.get(DASHBOARD_URL)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny access to dashboard with invalid token")
    void shouldDenyAccessWithInvalidToken() throws Exception {
        String invalidToken = generateInvalidToken();

        mockMvc.perform(MockMvcRequestBuilders.get(DASHBOARD_URL)
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}

