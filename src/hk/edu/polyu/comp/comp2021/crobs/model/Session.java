package hk.edu.polyu.comp.comp2021.crobs.model;

/** Stores the login state for one execution of Crobs. */
public final class Session {
    /** Most recently selected user, exposed only during an active login. */
    private User currentUser;
    /** Whether a user is currently logged in. */
    private boolean loggedIn;

    /** Creates a session with no active login. */
    public Session() {
    }

    /**
     * Reports whether a login is active.
     * @return whether a user is logged in
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Reads the user selected for the active login.
     * @return the logged-in user, or {@code null} after logout or before login
     */
    public User getCurrentUser() {
        return loggedIn ? currentUser : null;
    }

    /**
     * Activates the supplied user account for this session.
     * @param user the account to select
     * @throws IllegalArgumentException if the account is {@code null}
     */
    void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        currentUser = user;
        loggedIn = true;
    }

    /** Ends the active login so subsequent queries expose no current user. */
    void logout() {
        loggedIn = false;
    }
}
