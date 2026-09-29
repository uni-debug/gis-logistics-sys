package com.gis.logistics.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    private final JwtService jwt = new JwtService("unit-test-secret-key-0123456789", "gis-logistics", 3600);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwt);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenSetsAuthentication() throws Exception {
        String token = jwt.issue(99L, "staff");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicBoolean chained = new AtomicBoolean(false);
        FilterChain chain = (r, w) -> chained.set(true);

        filter.doFilter(req, res, chain);

        assertTrue(chained.get());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(99L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void missingTokenPassesThroughWithoutAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicBoolean chained = new AtomicBoolean(false);

        filter.doFilter(req, res, (r, w) -> chained.set(true));

        assertTrue(chained.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidTokenDoesNotThrowAndSetsNoAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer not-a-valid-token");
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertDoesNotThrow(() -> filter.doFilter(req, res, (r, w) -> { }));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
