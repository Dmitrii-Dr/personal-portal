package com.dmdr.personal.portal.util;

public final class SlugUtil {
    private SlugUtil() {}
    public static String toSlug(String input) {
        if (input == null) return null;
        return input.trim().toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }
}
