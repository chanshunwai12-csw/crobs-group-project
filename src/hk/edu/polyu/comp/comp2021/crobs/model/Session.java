package hk.edu.polyu.comp.comp2021.crobs.model;

/** Stores the login state for one execution of Crobs. */
public final class Session {
    private User currentUser;

    public Session() {
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        currentUser = user;
    }

    void logout() {
        currentUser = null;
    }
}
