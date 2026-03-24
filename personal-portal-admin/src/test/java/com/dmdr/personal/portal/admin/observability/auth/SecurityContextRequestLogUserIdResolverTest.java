package com.dmdr.personal.portal.admin.observability.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityContextRequestLogUserIdResolverTest {

    private final SecurityContextRequestLogUserIdResolver resolver = new SecurityContextRequestLogUserIdResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveUserId_shouldReturnNullWhenAuthenticationMissing() {
        assertThat(resolver.resolveUserId(new MockHttpServletRequest())).isNull();
    }

    @Test
    void resolveUserId_shouldReturnUuidPrincipalAsIs() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(resolver.resolveUserId(new MockHttpServletRequest())).isEqualTo(userId);
    }

    @Test
    void resolveUserId_shouldParseStringPrincipalWhenUuid() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(resolver.resolveUserId(new MockHttpServletRequest())).isEqualTo(userId);
    }

    @Test
    void resolveUserId_shouldReturnNullForAnonymousOrInvalidPrincipal() {
        UsernamePasswordAuthenticationToken anonymous =
            new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertThat(resolver.resolveUserId(new MockHttpServletRequest())).isNull();

        UsernamePasswordAuthenticationToken invalid =
            new UsernamePasswordAuthenticationToken("not-a-uuid", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(invalid);
        assertThat(resolver.resolveUserId(new MockHttpServletRequest())).isNull();
    }
}
