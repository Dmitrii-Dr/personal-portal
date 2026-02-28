package com.dmdr.personal.portal.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User Account Activation Security Tests")
class UserAccountActivationSecurityTest extends SecurityTestBase {

    @Test
    @DisplayName("Should deny access to /api/v1/user/account/activation/verification without token")
    void shouldDenyAccessToVerificationWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/user/account/activation/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should deny access to /api/v1/user/account/activation/code without token")
    void shouldDenyAccessToCodeWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/user/account/activation/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow access to /api/v1/user/account/activation/verification with token")
    void shouldAllowAccessToVerificationWithToken() throws Exception {
        String userToken = generateUserToken();

        int statusCode = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/user/account/activation/verification")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"code\":\"123456\"}"))
                .andReturn()
                .getResponse()
                .getStatus();

        assert statusCode != 401 && statusCode != 403 : "Security blocked the request with status " + statusCode;
    }

    @Test
    @DisplayName("Should allow access to /api/v1/user/account/activation/code with token")
    void shouldAllowAccessToCodeWithToken() throws Exception {
        String userToken = generateUserToken();

        int statusCode = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/user/account/activation/code")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andReturn()
                .getResponse()
                .getStatus();

        assert statusCode != 401 && statusCode != 403 : "Security blocked the request with status " + statusCode;
    }
}
