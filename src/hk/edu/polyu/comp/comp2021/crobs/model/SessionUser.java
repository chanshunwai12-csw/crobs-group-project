package hk.edu.polyu.comp.comp2021.crobs.model;

/** User information needed to decide what a schedule may display. */
public interface SessionUser {
    /** @return the logged-in user's ID */
    String getUserId();

    /** @return whether the user has admin privileges, including superadmin */
    boolean isAdmin();
}
