package com.dmdr.personal.portal.core.state;

public final class HomePageActiveHolder {

    private static volatile boolean isActive = false;

    private HomePageActiveHolder() {
    }

    public static boolean isActive() {
        return isActive;
    }

    public static void setActive(boolean active) {
        isActive = active;
    }
}
