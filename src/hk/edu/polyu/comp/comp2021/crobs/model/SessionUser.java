package hk.edu.polyu.comp.comp2021.crobs.model;

/** User information needed to decide what a schedule may display. */
public interface SessionUser {
    /**
     * Identifies the viewer when applying booking owner visibility.
     * @return the logged-in user's ID
     */
    String getUserId();

    /**
     * Indicates whether schedules may reveal all booking owner IDs to this user.
     * @return whether the user has admin privileges, including superadmin
     */
    boolean isAdmin();
}
