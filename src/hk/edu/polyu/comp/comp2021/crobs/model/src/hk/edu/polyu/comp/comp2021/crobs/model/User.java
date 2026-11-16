package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Objects;

public class User {

    private final String id;
    private final String name;
    private Role role;

    public User(String id, String name, Role role) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.role = Objects.requireNonNull(role, "role");
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

    public void setRole(Role role) {
        this.role = Objects.requireNonNull(role, "role");
    }

    public boolean isAdmin() {
        return role != Role.REGULAR;
    }

    public boolean isSuperAdmin() {
        return role == Role.SUPERADMIN;
    }
}