package hk.edu.polyu.comp.comp2021.crobs.model;

/** A user account in Crobs. */
public final class User implements SessionUser {
    /** Unique account ID. */
    private final String id;
    /** Account display name. */
    private final String name;
    /** Account access level or requested role name. */
    private Role role;

    /**
     * Creates an account with an ID, a display name and an access level.
     * @param id the unique account ID
     * @param name the display name, trimmed before storage
     * @param role the initial access level
     * @throws IllegalArgumentException if the ID or name is blank, or any argument is null
     */
    public User(String id, String name, Role role) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID must not be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name must not be empty.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        this.id = id;
        this.name = name.trim();
        this.role = role;
    }

    /**
     * Identifies this account in user and booking commands.
     * @return the account ID
     */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public String getUserId() {
        return getId();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAdmin() {
        return getRole().isAdmin();
    }

    /**
     * Supplies the name chosen when the account was created.
     * @return the display name
     */
    public String getName() {
        return name;
    }

    /**
     * Supplies the account's current access level.
     * @return the current role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Updates the access level after authorization by the model.
     * @param newRole the replacement role
     * @throws IllegalArgumentException if the role is {@code null}
     */
    void changeRole(Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        role = newRole;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return id + " " + name + " " + role;
    }
}
