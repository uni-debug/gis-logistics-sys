package com.gis.logistics.common.crypto;

/**
 * 手机号脱敏：13812341234 -> 138****1234，展示层统一调用。
 */
public final class PhoneMasker {

    private PhoneMasker() {
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        String prefix = phone.substring(0, 3);
        String suffix = phone.substring(phone.length() - 4);
        return prefix + "****" + suffix;
    }
}
