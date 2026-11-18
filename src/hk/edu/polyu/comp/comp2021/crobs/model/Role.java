package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Locale;

/** The access level assigned to a Crobs user. */
public enum Role {
    /** A user who manages their own bookings. */
    REGULAR,
    /** A user who also manages rooms and regular users. */
    ADMIN,
    /** The unique administrator who may change other users' roles. */
    SUPERADMIN;

    /**
     * Tests whether this role includes administrative privileges.
     * @return whether the role is admin or superadmin
     */
    public boolean isAdmin() {
        return this == ADMIN || this == SUPERADMIN;
    }

    /**
     * Parses a case-insensitive role name.
     * @param value the role name entered by a user
     * @return the matching role
     * @throws IllegalArgumentException if the name is null or unknown
     */
    public static Role fromCommand(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
