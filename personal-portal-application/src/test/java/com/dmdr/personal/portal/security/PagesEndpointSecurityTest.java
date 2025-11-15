package com.dmdr.personal.portal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Pages Endpoint Security Tests")
class PagesEndpointSecurityTest extends SecurityTestBase {

    private static final String PAGES_URL = "/api/v1/pages/about-me";

    @Test
    @DisplayName("Should allow access to pages with USER token")
    void shouldAllowAccessWithUserToken() throws Exception {
        String userToken = generateUserToken();

        mockMvc.perform(MockMvcRequestBuilders.get(PAGES_URL)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to pages with ADMIN token")
    void shouldAllowAccessWithAdminToken() throws Exception {
        String adminToken = generateAdminToken();

        mockMvc.perform(MockMvcRequestBuilders.get(PAGES_URL)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access to pages without token")
    void shouldDenyAccessWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(PAGES_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny access to pages with invalid token")
    void shouldDenyAccessWithInvalidToken() throws Exception {
        String invalidToken = generateInvalidToken();

        mockMvc.perform(MockMvcRequestBuilders.get(PAGES_URL)
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}

