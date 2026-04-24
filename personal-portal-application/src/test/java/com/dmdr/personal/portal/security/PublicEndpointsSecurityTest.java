package com.dmdr.personal.portal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Public Endpoints Security Tests")
class PublicEndpointsSecurityTest extends SecurityTestBase {

    @Test
    @DisplayName("Should allow access to /api/v1/auth/login without token")
    void shouldAllowAccessToLoginWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                // Endpoint is public, but invalid credentials return domain-level 401.
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PEC-410"));
    }

    @Test
    @DisplayName("Should allow access to /api/v1/auth/registry without token")
    void shouldAllowAccessToRegistryWithoutToken() throws Exception {
        int statusCode = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/registry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"registrytest" + System.currentTimeMillis()
                        + "@example.com\",\"password\":\"password123\","
                        + "\"firstName\":\"Test\",\"lastName\":\"User\","
                        + "\"phoneNumber\":\"+1234567890\",\"signedAgreements\":{}}"))
                .andReturn()
                .getResponse()
                .getStatus();

        // Should not be 401 (Unauthorized) or 403 (Forbidden) - security allows it
        assert statusCode != 401 && statusCode != 403 : "Security blocked the request with status " + statusCode;
    }

    @Test
    @DisplayName("Should deny access to /api/v1/auth/login with token (409 Conflict)")
    void shouldDenyAccessToLoginWithToken() throws Exception {
        String userToken = generateUserToken();

        // Authenticated users should get 409 Conflict when trying to access login
        // endpoint
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should deny access to /api/v1/auth/registry with token (409 Conflict)")
    void shouldDenyAccessToRegistryWithToken() throws Exception {
        String adminToken = generateAdminToken();

        // Authenticated users should get 409 Conflict when trying to access registry
        // endpoint
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/registry")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"registrytokentest" + System.currentTimeMillis()
                        + "@example.com\",\"password\":\"password123\","
                        + "\"firstName\":\"Test\",\"lastName\":\"User\","
                        + "\"phoneNumber\":\"+1234567890\",\"signedAgreements\":{}}"))
                .andExpect(status().isConflict());
    }

}
