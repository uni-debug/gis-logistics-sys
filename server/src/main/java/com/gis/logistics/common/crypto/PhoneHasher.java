package com.gis.logistics.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 手机号 sha256 哈希：用于精确检索与唯一约束（存 users.phone_hash）。
 */
public final class PhoneHasher {

    private PhoneHasher() {
    }

    public static String sha256Hex(String phone) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(phone.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("sha256 unavailable", e);
        }
    }
}
