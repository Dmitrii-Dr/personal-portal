package com.dmdr.personal.portal.core.security;

public enum SystemRole {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN");

    private final String authority;

    SystemRole(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

    public static SystemRole fromAuthority(String authority) {
        for (SystemRole role : values()) {
            if (role.authority.equalsIgnoreCase(authority)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown authority: " + authority);
        }
    }

