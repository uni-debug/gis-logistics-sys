package com.gis.logistics.common.crypto;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

class PhoneCryptoTest {

    private final byte[] key = new byte[32];

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        new SecureRandom().nextBytes(key);
    }

    @Test
    void encryptDecryptRoundTrip() {
        PhoneCipher cipher = PhoneCipher.fromKeyBytes(key);
        String encoded = cipher.encrypt("13812341234");
        assertNotEquals("13812341234", encoded);
        assertEquals("13812341234", cipher.decrypt(encoded));
    }

    @Test
    void tamperRejection() {
        PhoneCipher cipher = PhoneCipher.fromKeyBytes(key);
        String encoded = cipher.encrypt("13812341234");
        byte[] raw = java.util.Base64.getDecoder().decode(encoded);
        raw[raw.length - 1] ^= 0x01;
        String tampered = java.util.Base64.getEncoder().encodeToString(raw);
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    void wrongKeyRejected() {
        PhoneCipher a = PhoneCipher.fromKeyBytes(key);
        byte[] other = new byte[32];
        new SecureRandom().nextBytes(other);
        PhoneCipher b = PhoneCipher.fromKeyBytes(other);
        String encoded = a.encrypt("13812341234");
        assertThrows(IllegalStateException.class, () -> b.decrypt(encoded));
    }

    @Test
    void hashIsDeterministicAndHex64() {
        String h = PhoneHasher.sha256Hex("13812341234");
        assertEquals(64, h.length());
        assertEquals(h, PhoneHasher.sha256Hex("13812341234"));
        assertNotEquals(h, PhoneHasher.sha256Hex("13812341235"));
    }

    @Test
    void maskFormat() {
        assertEquals("138****1234", PhoneMasker.mask("13812341234"));
        assertEquals("ab", PhoneMasker.mask("ab"));
        assertNull(PhoneMasker.mask(null));
    }

    @Test
    void badKeySizeRejected() {
        assertThrows(IllegalArgumentException.class, () -> PhoneCipher.fromKeyBytes(new byte[8]));
    }
}
