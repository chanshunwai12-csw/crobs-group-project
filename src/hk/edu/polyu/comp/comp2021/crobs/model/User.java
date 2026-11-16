package hk.edu.polyu.comp.comp2021.crobs.model;

/** A user account in Crobs. */
public final class User {
    private final String id;
    private final String name;
    private Role role;

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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    /** Package-private so only the model can change access privileges. */
    void changeRole(Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        role = newRole;
    }

    @Override
    public String toString() {
        return id + " " + name + " " + role;
    }
}
