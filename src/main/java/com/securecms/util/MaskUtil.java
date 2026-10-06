package com.securecms.util;

public final class MaskUtil {

    private MaskUtil() {
    }

    /** "9876543210" -> "******3210" */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        if (phone.length() <= 4) {
            return "****";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
