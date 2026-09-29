package com.gis.logistics.common.security;

import com.gis.logistics.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwt = new JwtService("unit-test-secret-key-0123456789", "gis-logistics", 3600);

    @Test
    void issueAndParseRoundTrip() {
        String token = jwt.issue(42L, "staff");
        JwtService.Claims claims = jwt.parse(token);
        assertEquals(42L, claims.getUserId());
        assertEquals("staff", claims.getRole());
    }

    @Test
    void tamperedTokenRejected() {
        String token = jwt.issue(1L, "user");
        String tampered = token.substring(0, token.length() - 2) + "aa";
        assertThrows(BizException.class, () -> jwt.parse(tampered));
    }

    @Test
    void expiredTokenRejected() {
        JwtService shortLived = new JwtService("another-secret-key-0123456789abcd", "gis-logistics", -1);
        String token = shortLived.issue(7L, "admin");
        assertThrows(BizException.class, () -> shortLived.parse(token));
    }

    @Test
    void missingSecretFailsFast() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("", "gis-logistics", 3600));
    }
}
