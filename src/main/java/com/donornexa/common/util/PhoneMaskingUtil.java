package com.donornexa.common.util;

/**
 * Utility for masking sensitive personal phone numbers to preserve donor privacy.
 * Conforms to the CEP requirement: +92-300-***1234 format.
 */
public final class PhoneMaskingUtil {

    private PhoneMaskingUtil() {
        // Private constructor for utility class
    }

    /**
     * Masks the middle digits of a phone number.
     * E.g. "+92-300-2101001" -> "+92-300-***1001"
     *      "03001234567"     -> "0300***4567"
     *
     * @param phoneNumber Raw phone number string
     * @return Privacy-masked phone number
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "***-***-****";
        }

        String trimmed = phoneNumber.trim();
        int length = trimmed.length();

        if (length <= 6) {
            return "***-****";
        }

        // Keep first 4 characters and last 4 characters, mask the middle
        int prefixLen = Math.min(6, length - 4);
        int suffixLen = 4;
        String prefix = trimmed.substring(0, prefixLen);
        String suffix = trimmed.substring(length - suffixLen);

        return prefix + "-***" + suffix;
    }
}
