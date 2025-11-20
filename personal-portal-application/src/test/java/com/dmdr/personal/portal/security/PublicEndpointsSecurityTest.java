package com.dmdr.personal.portal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Public Endpoints Security Tests")
class PublicEndpointsSecurityTest extends SecurityTestBase {

    @Test
    @DisplayName("Should allow access to /api/v1/auth/login without token")
    void shouldAllowAccessToLoginWithoutToken() {
        // We are checking that /login API is available (not blocked by security) even
        // if login or password is incorrect
        // The endpoint may return business logic errors (4xx/5xx), but should not
        // return 401/403 security errors
        try {
            int statusCode = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            // Should not be 401 (Unauthorized) or 403 (Forbidden) - security allows it
            assert statusCode != 401 && statusCode != 403 : "Security blocked the request with status " + statusCode;
        } catch (Exception e) {
            // Exception is fine as long as it's not a security exception (401/403)
            // The endpoint may throw business logic exceptions, but security should allow
            // the request
            String message = e.getMessage() != null ? e.getMessage() : "";
            assert !message.contains("401") && !message.contains("403")
                    : "Security blocked the request: " + message;
        }
    }

    @Test
    @DisplayName("Should allow access to /api/v1/auth/registry without token")
    void shouldAllowAccessToRegistryWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/registry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"registrytest" + System.currentTimeMillis()
                        + "@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().is2xxSuccessful()); // Should succeed (201 Created) or 4xx (validation), not 401/403
    }


    @Test
    @DisplayName("Should deny access to /api/v1/auth/login with token (409 Conflict)")
    void shouldDenyAccessToLoginWithToken() throws Exception {
        String userToken = generateUserToken();

        // Authenticated users should get 409 Conflict when trying to access login endpoint
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

        // Authenticated users should get 409 Conflict when trying to access registry endpoint
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/registry")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"registrytokentest" + System.currentTimeMillis()
                        + "@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

}
