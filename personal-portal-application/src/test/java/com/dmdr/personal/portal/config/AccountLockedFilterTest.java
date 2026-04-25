package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountLockedFilterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccountLockedFilter filter = new AccountLockedFilter(userRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipPublicApiPaths() throws Exception {
        setAuthenticatedUser(UUID.randomUUID());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/pages/about-me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verifyNoInteractions(userRepository);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldAllowWhenAccountIsNotLocked() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);

        User user = new User();
        user.setLocked(false);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/booking");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verify(userRepository).findUserById(userId);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldBlockWhenAccountIsLocked() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthenticatedUser(userId);

        User user = new User();
        user.setLocked(true);
        when(userRepository.findUserById(userId)).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/booking");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verify(userRepository).findUserById(userId);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("PEC-422");
    }

    private void setAuthenticatedUser(UUID userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
