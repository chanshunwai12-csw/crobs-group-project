package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Locale;

/** The access level assigned to a Crobs user. */
public enum Role {
    REGULAR,
    ADMIN,
    SUPERADMIN;

    public boolean isAdmin() {
        return this == ADMIN || this == SUPERADMIN;
    }

    public static Role fromCommand(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
